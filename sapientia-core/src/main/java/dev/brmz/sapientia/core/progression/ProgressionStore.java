package dev.brmz.sapientia.core.progression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.core.persistence.DatabaseWorker;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQLite persistence for the server era ({@code server_state}) and player
 * discoveries ({@code player_discoveries}). Discoveries are queued and written
 * in batches by the database thread; reads run on the caller's thread.
 */
public final class ProgressionStore implements DatabaseWorker.Flushable {

    private static final String ERA_KEY = "era";

    private final Logger logger;
    private final DataSource dataSource;
    private final Object lock = new Object();
    private final Object flushLock = new Object();
    private List<Discovery> pending = new ArrayList<>();

    private record Discovery(UUID player, NamespacedKey item, long at) {}

    public ProgressionStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    /** The saved server era, or {@code null} if none was saved yet. */
    public @Nullable Era loadEra() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT state_value FROM server_state WHERE state_key = ?")) {
            ps.setString(1, ERA_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Era.of(Integer.parseInt(rs.getString(1)));
            }
        } catch (SQLException | NumberFormatException e) {
            logger.log(Level.WARNING, "Failed to read the server era", e);
        }
        return null;
    }

    public void saveEra(@NotNull Era era) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO server_state (state_key, state_value, updated_at) VALUES (?, ?, ?) "
                             + "ON CONFLICT(state_key) DO UPDATE SET state_value = excluded.state_value, "
                             + "updated_at = excluded.updated_at")) {
            ps.setString(1, ERA_KEY);
            ps.setString(2, Integer.toString(era.number()));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to save the server era", e);
        }
    }

    public @NotNull Set<NamespacedKey> loadDiscoveries(@NotNull UUID player) {
        Set<NamespacedKey> out = new HashSet<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT item_id FROM player_discoveries WHERE player_uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NamespacedKey key = NamespacedKey.fromString(rs.getString(1));
                    if (key != null) out.add(key);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load discoveries of " + player, e);
        }
        return out;
    }

    /** Queues a discovery for the next flush. Safe from any thread. */
    public void queueDiscovery(@NotNull UUID player, @NotNull NamespacedKey item) {
        synchronized (lock) {
            pending.add(new Discovery(player, item, System.currentTimeMillis()));
        }
    }

    @Override
    public void flush() {
        synchronized (flushLock) {
            List<Discovery> batch;
            synchronized (lock) {
                if (pending.isEmpty()) return;
                batch = pending;
                pending = new ArrayList<>();
            }
            try (Connection c = dataSource.getConnection()) {
                boolean autoCommit = c.getAutoCommit();
                c.setAutoCommit(false);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT OR IGNORE INTO player_discoveries (player_uuid, item_id, discovered_at) VALUES (?, ?, ?)")) {
                    for (Discovery d : batch) {
                        ps.setString(1, d.player().toString());
                        ps.setString(2, d.item().toString());
                        ps.setLong(3, d.at());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to write " + batch.size() + " discoveries; retrying", e);
                synchronized (lock) {
                    batch.addAll(pending);
                    pending = batch;
                }
            }
        }
    }
}
