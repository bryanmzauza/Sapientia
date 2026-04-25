package dev.brmz.sapientia.api.fluids;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable {@code (FluidType, amount)} pair (T-301 / 1.2.0). Amounts are in
 * <em>millibuckets</em> (mB): {@code 1 bucket = 1000 mB}. Tanks may hold at most
 * one fluid type at a time — there is no mixing in 1.2.0 (see ADR-015).
 */
public record FluidStack(@NotNull FluidType type, long amountMb) {

    public FluidStack {
        if (type == null) throw new IllegalArgumentException("type");
        if (amountMb < 0) throw new IllegalArgumentException("amountMb < 0: " + amountMb);
    }

    public boolean isEmpty() {
        return amountMb <= 0;
    }

    public @NotNull FluidStack withAmount(long newAmountMb) {
        return new FluidStack(type, newAmountMb);
    }
}
