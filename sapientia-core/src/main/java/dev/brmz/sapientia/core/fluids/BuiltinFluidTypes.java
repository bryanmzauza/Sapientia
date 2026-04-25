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

    private BuiltinFluidTypes() {}
}
