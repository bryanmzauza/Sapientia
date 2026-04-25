package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier fluid tank (T-301 / 1.2.0). 4 buckets / 4000 mB capacity. */
public final class SapientiaFluidTank extends FluidsContentBlock {
    public SapientiaFluidTank(@NotNull Plugin plugin) {
        super(plugin, "fluid_tank", Material.GLASS,
                "block.fluid_tank.name", FluidNodeType.TANK, EnergyTier.LOW);
    }
}
