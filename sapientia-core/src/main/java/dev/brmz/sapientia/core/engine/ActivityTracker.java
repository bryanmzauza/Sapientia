package dev.brmz.sapientia.core.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Feeds player positions into the {@link ActivityMap}. Only chunk-border
 * crossings update the map, so walking inside a chunk costs one integer
 * comparison per move event.
 */
public final class ActivityTracker implements Listener {

    private record ChunkPos(String world, int x, int z) {}

    private final ActivityMap map;
    private final LongSupplier clock;
    private final Map<UUID, ChunkPos> lastPosition = new HashMap<>();

    public ActivityTracker(@NotNull ActivityMap map, @NotNull LongSupplier clock) {
        this.map = map;
        this.clock = clock;
    }

    public @NotNull ActivityMap map() {
        return map;
    }

    /** Registers players already online (plugin reload). */
    public void seed(@NotNull Iterable<? extends Player> players) {
        for (Player player : players) {
            place(player, player.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        place(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        ChunkPos last = lastPosition.remove(event.getPlayer().getUniqueId());
        if (last != null) {
            map.removeViewer(last.world(), last.x(), last.z(), clock.getAsLong());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if ((to.getBlockX() >> 4) == (from.getBlockX() >> 4)
                && (to.getBlockZ() >> 4) == (from.getBlockZ() >> 4)
                && to.getWorld() == from.getWorld()) {
            return;
        }
        move(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        move(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        move(event.getPlayer(), event.getRespawnLocation());
    }

    private void place(Player player, Location location) {
        ChunkPos pos = toPos(location);
        ChunkPos previous = lastPosition.put(player.getUniqueId(), pos);
        long now = clock.getAsLong();
        if (previous != null) {
            map.moveViewer(previous.world(), previous.x(), previous.z(), pos.world(), pos.x(), pos.z(), now);
        } else {
            map.addViewer(pos.world(), pos.x(), pos.z(), now);
        }
    }

    private void move(Player player, Location to) {
        if (to == null || to.getWorld() == null) {
            return;
        }
        place(player, to);
    }

    private static ChunkPos toPos(Location location) {
        return new ChunkPos(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }
}
