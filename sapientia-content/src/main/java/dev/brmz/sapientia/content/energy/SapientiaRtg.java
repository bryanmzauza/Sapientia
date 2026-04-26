package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * HV-tier RTG (T-425 / 1.6.0). Trickle-charge generator running off radioactive
 * scrap. Decay-rate kinetic wiring lands with the 2.0.0 nuclear pass.
 */
public final class SapientiaRtg extends EnergyContentBlock {
    public SapientiaRtg(@NotNull Plugin plugin) {
        super(plugin, "rtg", Material.SEA_LANTERN,
                "block.rtg.name", EnergyNodeType.GENERATOR, EnergyTier.HIGH);
    }
}
