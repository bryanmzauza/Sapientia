package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier plate press: ingot → plate (T-404 / 1.4.0). */
public final class SapientiaPlatePress extends MachineEnergyBlock {
    public SapientiaPlatePress(@NotNull Plugin plugin) {
        super(plugin, "plate_press", Material.ANVIL,
                "block.plate_press.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
