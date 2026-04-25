package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier boiler (T-426 / 1.6.0). Vapourises a fluid input into a gas. */
public final class SapientiaBoiler extends MachineEnergyBlock {
    public SapientiaBoiler(@NotNull Plugin plugin) {
        super(plugin, "boiler", Material.FURNACE,
                "block.boiler.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
