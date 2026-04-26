package dev.brmz.sapientia.api.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;

/**
 * Tier-scaled fluid throughput and tank capacity (T-301 / 1.2.0).
 * Volumes in millibuckets (mB); 1 bucket = 1000 mB.
 */
public final class FluidSpecs {

    private FluidSpecs() {}

    /** Tank capacity per tier. LOW=4 buckets, MID=16, HIGH=64, EXTREME=256. */
    public static long capacityMb(EnergyTier tier) {
        return switch (tier) {
            case LOW -> 4_000L;
            case MID -> 16_000L;
            case HIGH -> 64_000L;
            case EXTREME -> 256_000L;
        };
    }

    /** Throughput per pump/drain per tick (mB). */
    public static long throughputPerTick(EnergyTier tier) {
        return switch (tier) {
            case LOW -> 50L;
            case MID -> 200L;
            case HIGH -> 800L;
            case EXTREME -> 3_200L;
        };
    }
}
