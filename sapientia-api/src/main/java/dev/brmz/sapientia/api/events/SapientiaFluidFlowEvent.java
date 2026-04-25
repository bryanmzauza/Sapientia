package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.fluids.FluidNetwork;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired once per fluid solver tick after volume has moved through a network
 * (T-301 / 1.2.0). Not cancellable; mutating the network during the event is
 * undefined behaviour. All amounts are in millibuckets (mB).
 */
public class SapientiaFluidFlowEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FluidNetwork network;
    private final long pumpedMb;
    private final long drainedMb;
    private final long bufferedMb;

    public SapientiaFluidFlowEvent(
            @NotNull FluidNetwork network, long pumpedMb, long drainedMb, long bufferedMb) {
        this.network = network;
        this.pumpedMb = pumpedMb;
        this.drainedMb = drainedMb;
        this.bufferedMb = bufferedMb;
    }

    public @NotNull FluidNetwork network() {
        return network;
    }

    public long pumpedMb() {
        return pumpedMb;
    }

    public long drainedMb() {
        return drainedMb;
    }

    public long bufferedMb() {
        return bufferedMb;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
