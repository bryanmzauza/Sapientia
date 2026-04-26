package dev.brmz.sapientia.api.energy;

import org.jetbrains.annotations.NotNull;

/**
 * Static throughput / capacity table for energy nodes by type + tier. Values are
 * placeholders calibrated for the 0.3.0 demo content; balancing happens later.
 *
 * <p>Lives in the public API so addons can sample the same numbers when they
 * tune custom node bookkeeping.
 */
public final class EnergySpecs {

    private EnergySpecs() {}

    /** Maximum buffer (capacity) for a node of the given type + tier, in E. */
    public static long bufferMax(@NotNull EnergyNodeType type, @NotNull EnergyTier tier) {
        return switch (type) {
            case GENERATOR -> 4L * tierMul(tier);
            case CABLE     -> tierMul(tier);
            case CAPACITOR -> 1_000L * tierMul(tier);
            case CONSUMER  -> 8L * tierMul(tier);
        };
    }

    /** How much a generator produces per energy tick. */
    public static long generationPerTick(@NotNull EnergyTier tier) {
        return 32L * tierMul(tier);
    }

    /** How much a consumer drains per energy tick. */
    public static long consumptionPerTick(@NotNull EnergyTier tier) {
        return 16L * tierMul(tier);
    }

    private static long tierMul(EnergyTier tier) {
        return switch (tier) {
            case LOW     -> 1L;
            case MID     -> 4L;
            case HIGH    -> 16L;
            case EXTREME -> 64L;
        };
    }
}
