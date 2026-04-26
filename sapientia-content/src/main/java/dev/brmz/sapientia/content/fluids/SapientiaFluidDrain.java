package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier fluid drain (T-301 / 1.2.0). Pushes fluid into adjacent cauldron / air. */
public final class SapientiaFluidDrain extends FluidsContentBlock {
    public SapientiaFluidDrain(@NotNull Plugin plugin) {
        super(plugin, "fluid_drain", Material.SMOKER,
                "block.fluid_drain.name", FluidNodeType.DRAIN, EnergyTier.LOW);
    }
}
