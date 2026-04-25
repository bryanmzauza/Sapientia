package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.logistics.ItemNetwork;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired once per logistics tick after the solver has moved items through a
 * network (T-300 / 1.1.0). Listeners get a snapshot of the totals; mutating
 * the network during the event is undefined behaviour. Not cancellable.
 */
public class SapientiaItemFlowEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemNetwork network;
    private final long produced;
    private final long consumed;
    private final long inTransit;

    public SapientiaItemFlowEvent(
            @NotNull ItemNetwork network, long produced, long consumed, long inTransit) {
        this.network = network;
        this.produced = produced;
        this.consumed = consumed;
        this.inTransit = inTransit;
    }

    public @NotNull ItemNetwork network() {
        return network;
    }

    /** Total items extracted from PRODUCER buffers this tick. */
    public long produced() {
        return produced;
    }

    /** Total items inserted into CONSUMER buffers this tick. */
    public long consumed() {
        return consumed;
    }

    /** Items that left a producer but did not reach a consumer (overflow / no route). */
    public long inTransit() {
        return inTransit;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
