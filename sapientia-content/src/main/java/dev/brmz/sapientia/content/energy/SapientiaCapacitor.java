package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier capacitor (bulk storage) (T-143). */
public final class SapientiaCapacitor extends EnergyContentBlock {
    public SapientiaCapacitor(@NotNull Plugin plugin) {
        super(plugin, "capacitor", Material.COPPER_BLOCK,
                "block.capacitor.name", EnergyNodeType.CAPACITOR, EnergyTier.LOW);
    }
}
