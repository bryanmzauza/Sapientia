package dev.brmz.sapientia.api.machine;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.jetbrains.annotations.NotNull;

/**
 * Voltage tier of a Sapientia machine, cable, or transformer (T-400 / 1.4.0).
 *
 * <p>Maps 1:1 to {@link EnergyTier} for solver-side throughput calculations,
 * but uses the canonical industrial naming (LV / MV / HV / EV) consumed by
 * Metallurgy content and the voltage-incompatibility policy (ADR-017).
 */
public enum MachineTier {
    /** Low voltage — basic copper/tin tier. Maps to {@link EnergyTier#LOW}. */
    LV(EnergyTier.LOW, 32L),
    /** Medium voltage — bronze/electrum tier. Maps to {@link EnergyTier#MID}. */
    MV(EnergyTier.MID, 128L),
    /** High voltage — steel/invar tier. Maps to {@link EnergyTier#HIGH}. */
    HV(EnergyTier.HIGH, 512L),
    /** Extreme voltage — kanthal/tungsten tier. Maps to {@link EnergyTier#EXTREME}. */
    EV(EnergyTier.EXTREME, 2048L);

    private final EnergyTier energyTier;
    private final long maxThroughput;

    MachineTier(@NotNull EnergyTier energyTier, long maxThroughput) {
        this.energyTier = energyTier;
        this.maxThroughput = maxThroughput;
    }

    /** The {@link EnergyTier} this machine tier corresponds to. */
    public @NotNull EnergyTier energyTier() {
        return energyTier;
    }

    /** Max throughput in SU/t for cables and machines at this tier. */
    public long maxThroughput() {
        return maxThroughput;
    }

    /** Looks up the {@link MachineTier} that matches the given {@link EnergyTier}. */
    public static @NotNull MachineTier fromEnergyTier(@NotNull EnergyTier tier) {
        return switch (tier) {
            case LOW -> LV;
            case MID -> MV;
            case HIGH -> HV;
            case EXTREME -> EV;
        };
    }
}
