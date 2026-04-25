package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidStack;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when fluid moves between two {@link FluidNode}s in the same network
 * (T-301 / 1.2.0). Not cancellable. Amount is in millibuckets (mB).
 */
public class SapientiaFluidTransferEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FluidNode from;
    private final FluidNode to;
    private final FluidStack stack;
    private final long amountMb;

    public SapientiaFluidTransferEvent(
            @NotNull FluidNode from, @NotNull FluidNode to,
            @NotNull FluidStack stack, long amountMb) {
        this.from = from;
        this.to = to;
        this.stack = stack;
        this.amountMb = amountMb;
    }

    public @NotNull FluidNode from() { return from; }
    public @NotNull FluidNode to()   { return to; }
    public @NotNull FluidStack stack() { return stack; }
    public long amountMb() { return amountMb; }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
