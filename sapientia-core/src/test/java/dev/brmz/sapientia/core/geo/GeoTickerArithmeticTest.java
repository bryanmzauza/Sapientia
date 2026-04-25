package dev.brmz.sapientia.core.geo;

import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-arithmetic invariants for the 1.7.1 geo &amp; atmosphere kinetic loop
 * (T-437). No Bukkit / no plugin instance — just the constants + pure helpers.
 */
final class GeoTickerArithmeticTest {

    @Test
    void allDrawConstantsArePositive() {
        assertTrue(GeoTicker.QUARRY_DRAW > 0L);
        assertTrue(GeoTicker.DRILL_RIG_DRAW > 0L);
        assertTrue(GeoTicker.DESALINATOR_DRAW > 0L);
        assertTrue(GeoTicker.GAS_EXTRACTOR_DRAW > 0L);
        assertTrue(GeoTicker.ATMO_COLLECTOR_DRAW > 0L);
    }

    @Test
    void drillRigCostsMoreEnergyThanQuarry() {
        // Drilling sub-bedrock is harder than open-pit mining.
        assertTrue(GeoTicker.DRILL_RIG_DRAW > GeoTicker.QUARRY_DRAW);
    }

    @Test
    void atmosphericAndDesalinatorShareDrawByDesign() {
        assertEquals(GeoTicker.ATMO_COLLECTOR_DRAW, GeoTicker.DESALINATOR_DRAW);
    }

    @Test
    void drillProbabilityIsAValidPermilInRange() {
        int p = GeoTicker.DRILL_HIT_PROB_PERMIL;
        assertTrue(p > 0 && p < 1000, "expected 0 < permil < 1000, got " + p);
    }

    @Test
    void desalinatorEfficiencyBetween80And100Percent() {
        // The 10% loss models the rock-salt residue (item-form deferred to 2.0.0).
        int input  = GeoTicker.DESALINATOR_INPUT_MB;
        int output = GeoTicker.DESALINATOR_OUTPUT_MB;
        assertTrue(input > output, "output must be < input (residue lost)");
        // efficiency = output * 100 / input (integer math; 90/100 → 90)
        int efficiencyPercent = output * 100 / input;
        assertTrue(efficiencyPercent >= 80, "efficiency should be ≥ 80%, got " + efficiencyPercent);
        assertTrue(efficiencyPercent < 100, "efficiency must be < 100% (residue), got " + efficiencyPercent);
    }

    @Test
    void atmosphericRoundRobinCyclesThroughThreeGases() {
        FluidType g0 = GeoTicker.atmoPick(0L);
        FluidType g1 = GeoTicker.atmoPick(1L);
        FluidType g2 = GeoTicker.atmoPick(2L);
        FluidType g3 = GeoTicker.atmoPick(3L); // wraps back to g0

        assertEquals(BuiltinFluidTypes.NITROGEN.id(),       g0.id());
        assertEquals(BuiltinFluidTypes.ARGON.id(),          g1.id());
        assertEquals(BuiltinFluidTypes.CARBON_DIOXIDE.id(), g2.id());
        assertEquals(g0.id(), g3.id());

        // Three distinct atmospheric gases.
        assertNotEquals(g0.id(), g1.id());
        assertNotEquals(g1.id(), g2.id());
        assertNotEquals(g0.id(), g2.id());
    }

    @Test
    void atmosphericRoundRobinHandlesNegativeCounter() {
        // Math.floorMod must treat -1 as bucket 2 (CARBON_DIOXIDE), not throw.
        FluidType picked = GeoTicker.atmoPick(-1L);
        assertEquals(BuiltinFluidTypes.CARBON_DIOXIDE.id(), picked.id());
    }

    @Test
    void allDrawsFitWithinSensibleHvUpperBound() {
        // The HV CONSUMER buffer is small (8 × tierMul) but refills from cables
        // each tick. Each per-cycle draw must stay within a sensible HV range so
        // a few ticks of charging are enough to drive one operation.
        long upperBound = 4096L;
        assertTrue(GeoTicker.QUARRY_DRAW       <= upperBound);
        assertTrue(GeoTicker.DRILL_RIG_DRAW    <= upperBound);
        assertTrue(GeoTicker.DESALINATOR_DRAW  <= upperBound);
        assertTrue(GeoTicker.GAS_EXTRACTOR_DRAW <= upperBound);
        assertTrue(GeoTicker.ATMO_COLLECTOR_DRAW <= upperBound);
    }
}
