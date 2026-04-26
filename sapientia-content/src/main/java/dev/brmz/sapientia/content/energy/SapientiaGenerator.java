package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier energy generator (T-143). */
public final class SapientiaGenerator extends EnergyContentBlock {
    public SapientiaGenerator(@NotNull Plugin plugin) {
        super(plugin, "generator", Material.FURNACE,
                "block.generator.name", EnergyNodeType.GENERATOR, EnergyTier.LOW);
    }
}
