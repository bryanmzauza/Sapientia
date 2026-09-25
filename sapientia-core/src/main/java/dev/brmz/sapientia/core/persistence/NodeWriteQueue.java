package dev.brmz.sapientia.core.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * Write-behind queue for one per-block table (energy, item or fluid nodes).
 * Callers enqueue upserts and deletes from any thread; the
 * {@link DatabaseWorker} writes them in one transaction per flush. Operations
 * on the same block collapse to the last one, so a buffer that changes every
 * tick costs one row write per flush.
 *
 * @param <R> the row snapshot bound into the upsert statement
 */
public final class NodeWriteQueue<R> implements DatabaseWorker.Flushable {

    /** Binds one upsert. */
    @FunctionalInterface
    public interface Binder<R> {
        void bind(@NotNull PreparedStatement statement, @NotNull BlockKey key, @NotNull R row) throws SQLException;
    }

    private static final Object DELETE = new Object();

    private final Logger logger;
    private final DataSource dataSource;
    private final String table;
    private final String upsertSql;
    private final Binder<R> binder;
    private final List<String> deleteSqls;
    private final Object lock = new Object();
    private final Object flushLock = new Object();
    private Map<BlockKey, Object> pending = new HashMap<>();

    /**
     * @param deleteSqls statements run for a deleted block, each taking
     *                   {@code world, x, y, z} as its four parameters
     */
    public NodeWriteQueue(@NotNull Logger logger, @NotNull DataSource dataSource, @NotNull String table,
                          @NotNull String upsertSql, @NotNull Binder<R> binder, @NotNull List<String> deleteSqls) {
        this.logger = logger;
        this.dataSource = dataSource;
        this.table = table;
        this.upsertSql = upsertSql;
        this.binder = binder;
        this.deleteSqls = List.copyOf(deleteSqls);
    }

    public void put(@NotNull BlockKey key, @NotNull R row) {
        synchronized (lock) {
            pending.put(key, row);
        }
    }

    public void delete(@NotNull BlockKey key) {
        synchronized (lock) {
            pending.put(key, DELETE);
        }
    }

    public int pendingCount() {
        synchronized (lock) {
            return pending.size();
        }
    }

    @Override
    public void flush() {
        synchronized (flushLock) {
            Map<BlockKey, Object> batch;
            synchronized (lock) {
                if (pending.isEmpty()) return;
                batch = pending;
                pending = new HashMap<>();
            }
            try {
                write(batch);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to write " + batch.size() + " " + table
                        + " rows; retrying on the next flush.", e);
                synchronized (lock) {
                    for (Map.Entry<BlockKey, Object> entry : batch.entrySet()) {
                        pending.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void write(Map<BlockKey, Object> batch) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            List<PreparedStatement> deletes = new ArrayList<>(deleteSqls.size());
            try (PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
                for (String sql : deleteSqls) {
                    deletes.add(connection.prepareStatement(sql));
                }
                for (Map.Entry<BlockKey, Object> entry : batch.entrySet()) {
                    BlockKey key = entry.getKey();
                    if (entry.getValue() == DELETE) {
                        for (PreparedStatement delete : deletes) {
                            delete.setString(1, key.world());
                            delete.setInt(2, key.x());
                            delete.setInt(3, key.y());
                            delete.setInt(4, key.z());
                            delete.addBatch();
                        }
                    } else {
                        binder.bind(upsert, key, (R) entry.getValue());
                        upsert.addBatch();
                    }
                }
                upsert.executeBatch();
                for (PreparedStatement delete : deletes) {
                    delete.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                for (PreparedStatement delete : deletes) {
                    delete.close();
                }
                connection.setAutoCommit(autoCommit);
            }
        }
    }
}
