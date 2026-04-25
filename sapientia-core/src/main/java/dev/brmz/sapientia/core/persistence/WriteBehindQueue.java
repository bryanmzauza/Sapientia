package dev.brmz.sapientia.core.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Batches {@code custom_blocks} mutations from the main (or region) thread and flushes
 * them to SQLite from a single worker thread every {@link #FLUSH_INTERVAL_MS} ms.
 * Dedupes concurrent writes per {@link BlockKey} with last-write-wins semantics.
 * See docs/persistence-schema.md §7 and ROADMAP 0.2.0 (T-111).
 */
public final class WriteBehindQueue {

    /** Flush cadence, in milliseconds. Matches ADR-006 bucketing philosophy. */
    public static final long FLUSH_INTERVAL_MS = 500L;

    private final Logger logger;
    private final DataSource dataSource;
    private final Map<BlockKey, Op> pending = new HashMap<>();
    private final Object lock = new Object();
    private final ScheduledExecutorService scheduler;

    public WriteBehindQueue(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sapientia-WriteBehind");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
                this::flushSafely, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void enqueuePut(@NotNull BlockKey key, @NotNull String itemId, @Nullable byte[] stateBlob) {
        synchronized (lock) {
            pending.put(key, Op.put(itemId, stateBlob));
        }
    }

    public void enqueueRemove(@NotNull BlockKey key) {
        synchronized (lock) {
            pending.put(key, Op.REMOVE);
        }
    }

    /** Number of pending operations. Intended for tests/metrics only. */
    public int pendingCount() {
        synchronized (lock) {
            return pending.size();
        }
    }

    /** Forces a flush and waits for it to complete. Safe to call from any thread. */
    public void flushNow() {
        flushSafely();
    }

    /** Drains remaining operations synchronously and stops the scheduler. */
    public void shutdown() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        flushSafely();
    }

    private void flushSafely() {
        Map<BlockKey, Op> batch;
        synchronized (lock) {
            if (pending.isEmpty()) {
                return;
            }
            batch = new HashMap<>(pending);
            pending.clear();
        }
        try {
            flushBatch(batch);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Write-behind flush failed; re-queueing " + batch.size()
                    + " operations.", e);
            synchronized (lock) {
                // Re-queue only keys that haven't been superseded in the meantime.
                for (Map.Entry<BlockKey, Op> entry : batch.entrySet()) {
                    pending.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Unexpected error during write-behind flush.", e);
        }
    }

    private void flushBatch(@NotNull Map<BlockKey, Op> batch) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement putPs = connection.prepareStatement(
                         "INSERT INTO custom_blocks(world, chunk_x, chunk_z, block_x, block_y, block_z,"
                                 + " item_id, state_blob, updated_at)"
                                 + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                 + " ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET"
                                 + " item_id = excluded.item_id,"
                                 + " state_blob = excluded.state_blob,"
                                 + " updated_at = excluded.updated_at");
                 PreparedStatement delPs = connection.prepareStatement(
                         "DELETE FROM custom_blocks"
                                 + " WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {

                long now = System.currentTimeMillis();
                for (Map.Entry<BlockKey, Op> entry : batch.entrySet()) {
                    BlockKey key = entry.getKey();
                    Op op = entry.getValue();
                    if (op.kind == OpKind.REMOVE) {
                        delPs.setString(1, key.world());
                        delPs.setInt(2, key.x());
                        delPs.setInt(3, key.y());
                        delPs.setInt(4, key.z());
                        delPs.addBatch();
                    } else {
                        putPs.setString(1, key.world());
                        putPs.setInt(2, key.chunkX());
                        putPs.setInt(3, key.chunkZ());
                        putPs.setInt(4, key.x());
                        putPs.setInt(5, key.y());
                        putPs.setInt(6, key.z());
                        putPs.setString(7, op.itemId);
                        if (op.stateBlob == null) {
                            putPs.setNull(8, java.sql.Types.BLOB);
                        } else {
                            putPs.setBytes(8, op.stateBlob);
                        }
                        putPs.setLong(9, now);
                        putPs.addBatch();
                    }
                }
                putPs.executeBatch();
                delPs.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private enum OpKind { PUT, REMOVE }

    private record Op(OpKind kind, String itemId, byte[] stateBlob) {
        static final Op REMOVE = new Op(OpKind.REMOVE, null, null);

        static Op put(String itemId, byte[] stateBlob) {
            return new Op(OpKind.PUT, itemId, stateBlob);
        }
    }
}
