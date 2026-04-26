package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier cable (T-406 / 1.4.0). 4× the throughput of {@code cable_t1}. */
public final class SapientiaCableT2 extends EnergyContentBlock {
    public SapientiaCableT2(@NotNull Plugin plugin) {
        super(plugin, "cable_t2", Material.END_ROD,
                "block.cable_t2.name", EnergyNodeType.CABLE, EnergyTier.MID);
    }
}
