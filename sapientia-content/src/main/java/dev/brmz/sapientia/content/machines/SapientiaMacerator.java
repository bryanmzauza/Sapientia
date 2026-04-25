package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier macerator: ore → 2× dust (T-404 / 1.4.0). */
public final class SapientiaMacerator extends MachineEnergyBlock {
    public SapientiaMacerator(@NotNull Plugin plugin) {
        super(plugin, "macerator", Material.GRINDSTONE,
                "block.macerator.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
