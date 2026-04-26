package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.android.AndroidNode;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code AndroidTicker} once per tick window per loaded android
 * (T-451 / 1.9.0). Cancellable: addons may veto the tick (e.g. radio
 * silence, debug pause).
 *
 * <p>1.9.0 ships the event scaffolding; the kinetic AI loop (crop / log /
 * ore scans, builder pattern execution, slayer melee, trader exchanges)
 * lands in 1.9.1. Until then, observers receive the event but the android
 * does no world side-effects beyond burning the tick budget.
 *
 * <p>{@link #instructionsConsumed()} is always {@code 1} in 1.9.0: the
 * design contract is "1 instruction / tick / android" (T-451). Future
 * motor-tier upgrades (T-454) raise this cap once the budget envelope is
 * validated by P-020 (T-459).
 */
public final class SapientiaAndroidTickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AndroidNode android;
    private final long tickCount;
    private final int instructionsConsumed;
    private boolean cancelled;

    public SapientiaAndroidTickEvent(@NotNull AndroidNode android,
                                     long tickCount,
                                     int instructionsConsumed) {
        this.android = android;
        this.tickCount = tickCount;
        this.instructionsConsumed = instructionsConsumed;
    }

    public @NotNull AndroidNode android() { return android; }
    public long tickCount() { return tickCount; }
    public int instructionsConsumed() { return instructionsConsumed; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
