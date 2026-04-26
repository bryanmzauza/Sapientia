package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier cable (T-425 / 1.6.0). 16x the throughput of {@code cable_t1}. */
public final class SapientiaCableT3 extends EnergyContentBlock {
    public SapientiaCableT3(@NotNull Plugin plugin) {
        super(plugin, "cable_t3", Material.LIGHTNING_ROD,
                "block.cable_t3.name", EnergyNodeType.CABLE, EnergyTier.HIGH);
    }
}
