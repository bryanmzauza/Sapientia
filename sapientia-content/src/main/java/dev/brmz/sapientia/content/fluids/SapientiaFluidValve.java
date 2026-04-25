package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fluid valve (T-443 / 1.8.0). Junction that gates flow between its two ends:
 * in 1.8.1 the valve toggles open/closed via {@code SapientiaBlockInteractEvent}
 * and a logic-program input. In 1.8.0 it ships as a {@code JUNCTION} so the
 * fluid network already places + routes through it (always-open behaviour).
 */
public final class SapientiaFluidValve extends FluidsContentBlock {
    public SapientiaFluidValve(@NotNull Plugin plugin) {
        super(plugin, "fluid_valve", Material.LEVER,
                "block.fluid_valve.name", FluidNodeType.JUNCTION, EnergyTier.LOW);
    }
}
