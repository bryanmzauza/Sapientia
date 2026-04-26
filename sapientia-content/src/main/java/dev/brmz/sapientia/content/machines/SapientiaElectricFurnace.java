package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier electric furnace: dust → ingot (T-404 / 1.4.0). */
public final class SapientiaElectricFurnace extends MachineEnergyBlock {
    public SapientiaElectricFurnace(@NotNull Plugin plugin) {
        super(plugin, "electric_furnace", Material.BLAST_FURNACE,
                "block.electric_furnace.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
