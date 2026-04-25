package dev.brmz.sapientia.core.fluids;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brmz.sapientia.api.fluids.FluidType;
import org.junit.jupiter.api.Test;

/** T-411 / 1.5.0: ensures the petroleum & basic-chemistry fluids are catalogued. */
class BuiltinFluidTypesTest {

    @Test
    void petrochemFluidsAreRegistered() {
        FluidType[] all = {
                BuiltinFluidTypes.WATER,
                BuiltinFluidTypes.LAVA,
                BuiltinFluidTypes.MILK,
                BuiltinFluidTypes.CRUDE_OIL,
                BuiltinFluidTypes.DIESEL,
                BuiltinFluidTypes.GASOLINE,
                BuiltinFluidTypes.LUBRICANT,
                BuiltinFluidTypes.NUTRIENT_BROTH,
        };
        assertThat(all).hasSize(8);
        for (FluidType t : all) {
            assertThat(t.id().getNamespace()).isEqualTo("sapientia");
            assertThat(t.displayKey()).startsWith("fluid.");
            assertThat(t.density()).isPositive();
        }
    }

    @Test
    void crudeOilIsHeavierThanGasoline() {
        // Density informs the upcoming gravity / vertical-flow pass; sanity
        // check that crude is denser than gasoline.
        assertThat(BuiltinFluidTypes.CRUDE_OIL.density())
                .isGreaterThan(BuiltinFluidTypes.GASOLINE.density());
    }

    @Test
    void noFluidIdsCollide() {
        FluidType[] all = {
                BuiltinFluidTypes.WATER, BuiltinFluidTypes.LAVA, BuiltinFluidTypes.MILK,
                BuiltinFluidTypes.CRUDE_OIL, BuiltinFluidTypes.DIESEL,
                BuiltinFluidTypes.GASOLINE, BuiltinFluidTypes.LUBRICANT,
                BuiltinFluidTypes.NUTRIENT_BROTH,
        };
        java.util.Set<org.bukkit.NamespacedKey> ids = new java.util.HashSet<>();
        for (FluidType t : all) {
            assertThat(ids.add(t.id())).as("duplicate id %s", t.id()).isTrue();
        }
    }
}
