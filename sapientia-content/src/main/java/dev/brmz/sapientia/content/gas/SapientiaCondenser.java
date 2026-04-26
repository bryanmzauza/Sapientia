package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier condenser (T-426 / 1.6.0). Returns a gas to its liquid form. */
public final class SapientiaCondenser extends MachineEnergyBlock {
    public SapientiaCondenser(@NotNull Plugin plugin) {
        super(plugin, "condenser", Material.SOUL_CAMPFIRE,
                "block.condenser.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
