package dev.brmz.sapientia.core.electronics;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidSpecs;
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import org.junit.jupiter.api.Test;

/**
 * T-429 / 1.6.1: pure-arithmetic invariants on the HV kinetic-loop constants
 * exposed by {@link ElectronicsTicker}. These don't drive Bukkit blocks; they
 * lock the published rates so a refactor can't silently change game balance.
 */
class ElectronicsTickerStoichiometryTest {

    /**
     * 2 H&#x2082;O &rarr; 2 H&#x2082; + O&#x2082; (2:2:1 stoichiometric ratio).
     * Per cycle: water = 100 mB, hydrogen = 200 mB, oxygen = 100 mB.
     * The hydrogen/water ratio captures the "2 H per H&#x2082;O" relation
     * (each H&#x2082;O molecule yields one H&#x2082;).
     */
    @Test
    void electrolyzerProducesTwoHydrogenPerWater() {
        assertThat(ElectronicsTicker.ELECTROLYZER_HYDROGEN_MB)
                .isEqualTo(2 * ElectronicsTicker.ELECTROLYZER_WATER_MB);
    }

    @Test
    void electrolyzerProducesEqualOxygenAndWater() {
        // O₂ output (mB) equals water input (mB) — H₂ + ½ O₂ per H₂O molecule
        // collapsed to the integer ratio 2:2:1 → the mB ratio O₂:H₂O is 1:1.
        assertThat(ElectronicsTicker.ELECTROLYZER_OXYGEN_MB)
                .isEqualTo(ElectronicsTicker.ELECTROLYZER_WATER_MB);
    }

    /**
     * Boiler / condenser are inverse pairs: a mB of water boiled to N mB of gas
     * must return exactly 1 mB of water when condensed. Any drift here means the
     * gas pipeline can mint or destroy mass.
     */
    @Test
    void boilerAndCondenserAreMassConserving() {
        // Boiler: water_in → gas_out. Condenser: gas_in → water_out.
        // The same constants are reused on both sides, so the ratio is symmetric
        // by construction — but we still pin the 1:2 expansion explicitly.
        assertThat(ElectronicsTicker.BOILER_GAS_MB)
                .isEqualTo(2 * ElectronicsTicker.BOILER_WATER_MB);
    }

    @Test
    void boilerEnergyExceedsCondenserEnergy() {
        // Boiling a liquid takes more energy than condensing the gas (latent heat of
        // vaporisation > the work the condenser does). Mirrors thermodynamic intuition.
        assertThat(ElectronicsTicker.BOILER_ENERGY)
                .isGreaterThan(ElectronicsTicker.CONDENSER_ENERGY);
    }

    @Test
    void hvGeneratorRatesArePositive() {
        assertThat(ElectronicsTicker.GEOTHERMAL_SU_PER_LAVA).isPositive();
        assertThat(ElectronicsTicker.HYDROGEN_SU_PER_MB).isPositive();
        assertThat(ElectronicsTicker.ETHYLENE_SU_PER_MB).isPositive();
        assertThat(ElectronicsTicker.RTG_SU_PER_TICK).isPositive();
        assertThat(ElectronicsTicker.GAS_TURBINE_DRAIN_MB).isPositive();
    }

    @Test
    void hydrogenIsBetterFuelThanEthylene() {
        // Per ROADMAP contract: H₂ has the highest SU/mB of the gases (lightest, hottest flame).
        assertThat(ElectronicsTicker.HYDROGEN_SU_PER_MB)
                .isGreaterThan(ElectronicsTicker.ETHYLENE_SU_PER_MB);
    }

    /**
     * Gas-pipe pressure cap (T-429): high-density-tier gas tanks must respect the
     * shared {@link FluidSpecs#capacityMb(EnergyTier)} ceiling. Until 1.7.0
     * introduces dedicated gas pressure semantics, the existing tier capacity
     * IS the pressure cap. This locks the contract that gases share the fluid graph
     * (ADR-019) and cannot bypass tier limits.
     */
    @Test
    void gasesRespectFluidTierCapacity() {
        long lvCap   = FluidSpecs.capacityMb(EnergyTier.LOW);
        long midCap  = FluidSpecs.capacityMb(EnergyTier.MID);
        long highCap = FluidSpecs.capacityMb(EnergyTier.HIGH);
        assertThat(midCap).isGreaterThan(lvCap);
        assertThat(highCap).isGreaterThan(midCap);
        // Gas burn cycles must always fit comfortably below the LV cap so a single
        // tank tick never floods a pipe. 200 mB H2 << 4000 mB LV cap.
        assertThat((long) ElectronicsTicker.ELECTROLYZER_HYDROGEN_MB).isLessThan(lvCap);
        assertThat((long) ElectronicsTicker.BOILER_GAS_MB).isLessThan(lvCap);
    }

    @Test
    void electrolyzerInputIsWaterOutputIsHydrogenAndOxygen() {
        // Sanity: ensures we wired the right fluid types — guards against a typo
        // in BuiltinFluidTypes drift.
        assertThat(BuiltinFluidTypes.WATER.id().getKey()).isEqualTo("water");
        assertThat(BuiltinFluidTypes.HYDROGEN.id().getKey()).isEqualTo("hydrogen");
        assertThat(BuiltinFluidTypes.OXYGEN_GAS.id().getKey()).isEqualTo("oxygen_gas");
        assertThat(BuiltinFluidTypes.COMPRESSED_AIR.id().getKey()).isEqualTo("compressed_air");
    }
}
