package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.machine.Machine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by the Sapientia core once per machine tick after the machine's own
 * scheduled work has run. Snapshot only; not cancellable.
 */
public class SapientiaMachineTickEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Machine machine;
    private final long tick;

    public SapientiaMachineTickEvent(@NotNull Machine machine, long tick) {
        this.machine = machine;
        this.tick = tick;
    }

    public @NotNull Machine machine() {
        return machine;
    }

    /** Server tick counter at the time of dispatch. */
    public long tick() {
        return tick;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
