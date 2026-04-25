package dev.brmz.sapientia.content.chemistry;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier still (T-414 / 1.5.0). Distills fermented mash into purified fluids. */
public final class SapientiaStill extends MachineEnergyBlock {
    public SapientiaStill(@NotNull Plugin plugin) {
        super(plugin, "still", Material.SMOKER,
                "block.still.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
