package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired once per energy tick after the solver has distributed energy through a
 * network. Listeners get a snapshot of the totals; mutating the network during
 * the event is undefined behaviour. Not cancellable.
 */
public class SapientiaEnergyFlowEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EnergyNetwork network;
    private final long generated;
    private final long consumed;
    private final long stored;

    public SapientiaEnergyFlowEvent(
            @NotNull EnergyNetwork network, long generated, long consumed, long stored) {
        this.network = network;
        this.generated = generated;
        this.consumed = consumed;
        this.stored = stored;
    }

    public @NotNull EnergyNetwork network() {
        return network;
    }

    /** Total energy produced in this tick (before consumption). */
    public long generated() {
        return generated;
    }

    /** Total energy actually delivered to consumers in this tick. */
    public long consumed() {
        return consumed;
    }

    /** Total energy now buffered in the network. */
    public long stored() {
        return stored;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
