package dev.brmz.sapientia.core.platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.events.SapientiaPlayerPlatformDetectEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves and caches {@link PlatformType} per player. Uses Floodgate when present,
 * persists the result in SQLite. See ADR-002.
 */
public final class PlatformService {

    private final Logger logger;
    private final DataSource dataSource;
    private final boolean floodgateAvailable;
    private final Map<UUID, PlatformType> cache = new ConcurrentHashMap<>();

    public PlatformService(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
        this.floodgateAvailable = Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    public boolean floodgateAvailable() {
        return floodgateAvailable;
    }

    public @NotNull PlatformType resolve(@NotNull Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlatformType cached = loadCached(uuid);
            if (cached != null) {
                return cached;
            }
            PlatformType detected = detect(uuid);
            persist(uuid, detected);
            return detected;
        });
    }

    /**
     * Resolves the player's platform and fires {@link SapientiaPlayerPlatformDetectEvent}
     * exactly once per call. Intended to be invoked from a {@code PlayerJoinEvent}
     * handler. The {@code fromCache} flag reflects whether the value came from the
     * SQLite cache (true) or was freshly detected via Floodgate (false).
     */
    public @NotNull PlatformType resolveAndEmit(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PlatformType cached = cache.get(uuid);
        if (cached != null) {
            Bukkit.getPluginManager().callEvent(
                    new SapientiaPlayerPlatformDetectEvent(player, cached, true));
            return cached;
        }
        PlatformType fromDb = loadCached(uuid);
        if (fromDb != null) {
            cache.put(uuid, fromDb);
            Bukkit.getPluginManager().callEvent(
                    new SapientiaPlayerPlatformDetectEvent(player, fromDb, true));
            return fromDb;
        }
        PlatformType detected = detect(uuid);
        persist(uuid, detected);
        cache.put(uuid, detected);
        Bukkit.getPluginManager().callEvent(
                new SapientiaPlayerPlatformDetectEvent(player, detected, false));
        return detected;
    }

    public @NotNull PlatformType cachedOrDefault(@NotNull UUID uuid) {
        return cache.getOrDefault(uuid, PlatformType.JAVA);
    }

    private @NotNull PlatformType detect(@NotNull UUID uuid) {
        if (!floodgateAvailable) {
            return PlatformType.JAVA;
        }
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Boolean isBedrock = (Boolean) api.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(instance, uuid);
            return Boolean.TRUE.equals(isBedrock) ? PlatformType.BEDROCK : PlatformType.JAVA;
        } catch (ReflectiveOperationException e) {
            logger.log(Level.FINE, "Floodgate lookup failed; defaulting to JAVA", e);
            return PlatformType.JAVA;
        }
    }

    private PlatformType loadCached(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT platform FROM player_platform WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try {
                        return PlatformType.valueOf(rs.getString(1));
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read player_platform cache", e);
        }
        return null;
    }

    private void persist(UUID uuid, PlatformType platform) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO player_platform(uuid, platform, detected_at) "
                             + "VALUES (?, ?, ?) "
                             + "ON CONFLICT(uuid) DO UPDATE SET "
                             + "platform = excluded.platform, detected_at = excluded.detected_at")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, platform.name());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist player_platform cache", e);
        }
    }
}
