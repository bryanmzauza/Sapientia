package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier extractor: ingot → wire (T-404 / 1.4.0). */
public final class SapientiaExtractor extends MachineEnergyBlock {
    public SapientiaExtractor(@NotNull Plugin plugin) {
        super(plugin, "extractor", Material.OBSERVER,
                "block.extractor.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
