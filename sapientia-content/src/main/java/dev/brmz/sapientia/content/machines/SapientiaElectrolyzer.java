package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier electrolyzer (T-423 / 1.6.0). Splits water into hydrogen + oxygen. */
public final class SapientiaElectrolyzer extends MachineEnergyBlock {
    public SapientiaElectrolyzer(@NotNull Plugin plugin) {
        super(plugin, "electrolyzer", Material.BREWING_STAND,
                "block.electrolyzer.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
