package dev.brmz.sapientia.api.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.jetbrains.annotations.NotNull;

/**
 * Static throughput table for item logistics nodes by tier (T-300 / 1.1.0).
 * Values are calibrated for the 1.1.0 demo content; balancing happens via the
 * existing YAML override pipeline (0.5.0).
 *
 * <p>Throughput is expressed in <em>items per tick</em> (consistent with
 * {@code EnergySpecs}). At 20 TPS, LOW = 1280 items/s, EXTREME = 81920 items/s.
 */
public final class ItemSpecs {

    private ItemSpecs() {}

    /** Maximum items a node may move into / out of the network per tick. */
    public static int throughputPerTick(@NotNull EnergyTier tier) {
        return switch (tier) {
            case LOW     -> 64;
            case MID     -> 256;
            case HIGH    -> 1024;
            case EXTREME -> 4096;
        };
    }
}
