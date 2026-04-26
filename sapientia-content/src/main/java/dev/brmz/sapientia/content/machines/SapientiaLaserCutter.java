package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier laser cutter (T-423 / 1.6.0). Cuts silicon wafer + circuits. */
public final class SapientiaLaserCutter extends MachineEnergyBlock {
    public SapientiaLaserCutter(@NotNull Plugin plugin) {
        super(plugin, "laser_cutter", Material.STONECUTTER,
                "block.laser_cutter.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
