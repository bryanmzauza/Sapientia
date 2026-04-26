package dev.brmz.sapientia.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired right before a logic program executes a tick (T-302 / 1.3.0).
 * Cancelling the event skips this tick — the program remains registered and
 * enabled, it simply does not run for the current tick.
 */
public final class SapientiaLogicTickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String programName;
    private final long executionCount;
    private boolean cancelled;

    public SapientiaLogicTickEvent(@NotNull String programName, long executionCount) {
        this.programName = programName;
        this.executionCount = executionCount;
    }

    public @NotNull String programName() {
        return programName;
    }

    /** Total number of ticks this program has executed prior to this one. */
    public long executionCount() {
        return executionCount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
