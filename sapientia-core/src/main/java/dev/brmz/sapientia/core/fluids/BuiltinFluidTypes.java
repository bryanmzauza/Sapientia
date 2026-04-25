package dev.brmz.sapientia.core.fluids;

import dev.brmz.sapientia.api.fluids.FluidType;
import org.bukkit.NamespacedKey;

/**
 * Built-in fluid catalog (T-301 / 1.2.0). Registered automatically by
 * {@code SapientiaPlugin} into {@link FluidServiceImpl}.
 */
public final class BuiltinFluidTypes {

    public static final FluidType WATER = new FluidType(
            new NamespacedKey("sapientia", "water"), "fluid.water.name", 0x3F76E4, 1000, false);

    public static final FluidType LAVA = new FluidType(
            new NamespacedKey("sapientia", "lava"), "fluid.lava.name", 0xFF6A00, 3000, true);

    public static final FluidType MILK = new FluidType(
            new NamespacedKey("sapientia", "milk"), "fluid.milk.name", 0xF5F5F5, 1000, false);

    // --- Petroleum & basic chemistry (T-411 / 1.5.0) -----------------------------------------

    /** Black, dense crude pumped from underground reservoirs. Refined into 3 fractions + tar. */
    public static final FluidType CRUDE_OIL = new FluidType(
            new NamespacedKey("sapientia", "crude_oil"), "fluid.crude_oil.name", 0x1A1A1A, 870, false);

    /** Heavier petroleum fraction. Fuel for combustion generator. */
    public static final FluidType DIESEL = new FluidType(
            new NamespacedKey("sapientia", "diesel"), "fluid.diesel.name", 0xC2A24A, 832, false);

    /** Lighter petroleum fraction. High-output combustion fuel. */
    public static final FluidType GASOLINE = new FluidType(
            new NamespacedKey("sapientia", "gasoline"), "fluid.gasoline.name", 0xE6C97A, 750, false);

    /** Lubricant fraction. Reduces machine wear (1.6.0) and feeds the cracker. */
    public static final FluidType LUBRICANT = new FluidType(
            new NamespacedKey("sapientia", "lubricant"), "fluid.lubricant.name", 0x6B4F1F, 900, false);

    /** Nutrient broth produced by the bioreactor. Input to the fermenter for biogas. */
    public static final FluidType NUTRIENT_BROTH = new FluidType(
            new NamespacedKey("sapientia", "nutrient_broth"), "fluid.nutrient_broth.name", 0x6E8B3D, 1050, false);

    // --- Gases (T-426 / 1.6.0) ---------------------------------------------------------------
    // Per ADR-019, gases are FluidTypes with a density < 100 kg/m³. The fluid graph
    // currently treats them as low-density fluids; the dedicated gas-pressure pass
    // arrives with the 1.6.1 kinetic loop.

    /** Hydrogen — output of the electrolyzer. Fuel for the gas turbine. */
    public static final FluidType HYDROGEN = new FluidType(
            new NamespacedKey("sapientia", "hydrogen"), "fluid.hydrogen.name", 0xCCEEFF, 1, false);

    /** Oxygen gas — paired output of the electrolyzer. Required for combustion boost. */
    public static final FluidType OXYGEN_GAS = new FluidType(
            new NamespacedKey("sapientia", "oxygen_gas"), "fluid.oxygen_gas.name", 0x99CCFF, 1, false);

    /** Nitrogen — atmospheric collector output. Inert reagent in the chemical reactor. */
    public static final FluidType NITROGEN = new FluidType(
            new NamespacedKey("sapientia", "nitrogen"), "fluid.nitrogen.name", 0xAFCCDD, 1, false);

    /** Chlorine — chemical reactor output. Reagent in PVC + HCl chains. */
    public static final FluidType CHLORINE = new FluidType(
            new NamespacedKey("sapientia", "chlorine"), "fluid.chlorine.name", 0xC0E060, 3, false);

    /** Ethylene — cracker output. Polymerises into plastics in the chemical reactor. */
    public static final FluidType ETHYLENE = new FluidType(
            new NamespacedKey("sapientia", "ethylene"), "fluid.ethylene.name", 0xE0E0E0, 1, false);

    /** Compressed air — gas compressor output. Generic working gas for pneumatics. */
    public static final FluidType COMPRESSED_AIR = new FluidType(
            new NamespacedKey("sapientia", "compressed_air"), "fluid.compressed_air.name", 0xDDEEFF, 12, false);

    private BuiltinFluidTypes() {}
}
