package dev.brmz.sapientia.core.fluids;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brmz.sapientia.api.fluids.FluidType;
import org.junit.jupiter.api.Test;

/**
 * T-435 / 1.7.0: catalogue contract for the new atmospheric gases and
 * cryogenic liquids. Pure-arithmetic tests (no Bukkit) that lock the published
 * fluid invariants for the geo &amp; atmosphere release.
 */
class GeoAndAtmosphereFluidsTest {

    @Test
    void argonAndCarbonDioxideHaveGasDensity() {
        // Per ADR-019, gases carry density < 100 kg/m³ so the fluid graph
        // can route them as low-density flow until the dedicated pressure
        // pass arrives. Both atmospheric outputs must satisfy this gate.
        assertThat(BuiltinFluidTypes.ARGON.density()).isLessThan(100);
        assertThat(BuiltinFluidTypes.CARBON_DIOXIDE.density()).isLessThan(100);
    }

    @Test
    void liquidOxygenIsDenserThanWater() {
        // Liquid oxygen is a cryogenic LIQUID, not a gas — its density must
        // sit well above 100 kg/m³. This guards the "low-density => gas"
        // routing rule from misclassifying LOX.
        assertThat(BuiltinFluidTypes.LIQUID_OXYGEN.density())
                .isGreaterThan(BuiltinFluidTypes.WATER.density());
    }

    @Test
    void newFluidIdsAreUniqueAndNamespaced() {
        FluidType[] all = {
                BuiltinFluidTypes.ARGON,
                BuiltinFluidTypes.CARBON_DIOXIDE,
                BuiltinFluidTypes.LIQUID_OXYGEN,
        };
        for (FluidType t : all) {
            assertThat(t.id().getNamespace()).isEqualTo("sapientia");
            assertThat(t.id().getKey()).isNotBlank();
            assertThat(t.displayKey()).startsWith("fluid.").endsWith(".name");
            assertThat(t.hot()).isFalse();
        }
        assertThat(BuiltinFluidTypes.ARGON.id().getKey()).isEqualTo("argon");
        assertThat(BuiltinFluidTypes.CARBON_DIOXIDE.id().getKey()).isEqualTo("carbon_dioxide");
        assertThat(BuiltinFluidTypes.LIQUID_OXYGEN.id().getKey()).isEqualTo("liquid_oxygen");
    }

    @Test
    void newFluidsHaveNonZeroColor() {
        // Color must be non-zero so the renderer doesn't fall back to black.
        assertThat(BuiltinFluidTypes.ARGON.color()).isNotZero();
        assertThat(BuiltinFluidTypes.CARBON_DIOXIDE.color()).isNotZero();
        assertThat(BuiltinFluidTypes.LIQUID_OXYGEN.color()).isNotZero();
    }
}
