package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier fluid pipe (T-301 / 1.2.0). */
public final class SapientiaFluidPipe extends FluidsContentBlock {
    public SapientiaFluidPipe(@NotNull Plugin plugin) {
        super(plugin, "fluid_pipe", Material.IRON_BARS,
                "block.fluid_pipe.name", FluidNodeType.PIPE, EnergyTier.LOW);
    }
}
