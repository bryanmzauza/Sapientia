package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier cable connecting energy nodes (T-143). */
public final class SapientiaCable extends EnergyContentBlock {
    public SapientiaCable(@NotNull Plugin plugin) {
        super(plugin, "cable", Material.IRON_BARS,
                "block.cable.name", EnergyNodeType.CABLE, EnergyTier.LOW);
    }
}
