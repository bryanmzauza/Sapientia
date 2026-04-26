package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier fluid pump (T-301 / 1.2.0). Pulls from adjacent water/lava/cauldron. */
public final class SapientiaFluidPump extends FluidsContentBlock {
    public SapientiaFluidPump(@NotNull Plugin plugin) {
        super(plugin, "fluid_pump", Material.BLAST_FURNACE,
                "block.fluid_pump.name", FluidNodeType.PUMP, EnergyTier.LOW);
    }
}
