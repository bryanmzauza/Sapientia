package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier ore washer: dust → cleaned dust (T-404 / 1.4.0). */
public final class SapientiaOreWasher extends MachineEnergyBlock {
    public SapientiaOreWasher(@NotNull Plugin plugin) {
        super(plugin, "ore_washer", Material.CAULDRON,
                "block.ore_washer.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
