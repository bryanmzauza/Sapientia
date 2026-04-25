package dev.brmz.sapientia.core.guide;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.guide.UnlockService;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * SQLite-backed {@link UnlockService} (T-151 / 0.4.0). Reads go through an
 * in-memory cache keyed by player UUID; writes insert directly into the
 * {@code unlocked_content} table (low volume — unlocks are rare).
 */
public final class UnlockServiceImpl implements UnlockService {

    private final Logger logger;
    private final DataSource dataSource;
    private final Map<UUID, Set<NamespacedKey>> cache = new ConcurrentHashMap<>();

    public UnlockServiceImpl(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    @Override
    public boolean unlock(@NotNull UUID player, @NotNull NamespacedKey entryId) {
        Set<NamespacedKey> set = cache.computeIfAbsent(player, this::loadFromDb);
        if (!set.add(entryId)) return false;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO unlocked_content(player_uuid, entry_id, unlocked_at) "
                             + "VALUES (?, ?, ?)")) {
            ps.setString(1, player.toString());
            ps.setString(2, entryId.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to persist unlock for " + player + " / " + entryId, e);
            return true; // cache still reflects it
        }
    }

    @Override
    public boolean isUnlocked(@NotNull UUID player, @NotNull NamespacedKey entryId) {
        return cache.computeIfAbsent(player, this::loadFromDb).contains(entryId);
    }

    @Override
    public @NotNull Set<NamespacedKey> unlockedFor(@NotNull UUID player) {
        return Collections.unmodifiableSet(new HashSet<>(cache.computeIfAbsent(player, this::loadFromDb)));
    }

    public void evict(@NotNull UUID player) {
        cache.remove(player);
    }

    private Set<NamespacedKey> loadFromDb(UUID player) {
        Set<NamespacedKey> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT entry_id FROM unlocked_content WHERE player_uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Optional.ofNullable(NamespacedKey.fromString(rs.getString(1))).ifPresent(set::add);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load unlocks for " + player, e);
        }
        return set;
    }
}
