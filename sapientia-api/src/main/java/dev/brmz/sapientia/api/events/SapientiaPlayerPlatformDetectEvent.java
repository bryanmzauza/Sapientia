package dev.brmz.sapientia.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import dev.brmz.sapientia.api.PlatformType;

/**
 * Fired once per session when the platform of a joining player is resolved.
 * Not cancellable.
 */
public class SapientiaPlayerPlatformDetectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlatformType platform;
    private final boolean fromCache;

    public SapientiaPlayerPlatformDetectEvent(
            @NotNull Player player,
            @NotNull PlatformType platform,
            boolean fromCache) {
        this.player = player;
        this.platform = platform;
        this.fromCache = fromCache;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull PlatformType platform() {
        return platform;
    }

    /** True when the platform was resolved from the SQLite cache. */
    public boolean fromCache() {
        return fromCache;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
