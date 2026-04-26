package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier bench saw: rod → gear, rod → screws (T-404 / 1.4.0). */
public final class SapientiaBenchSaw extends MachineEnergyBlock {
    public SapientiaBenchSaw(@NotNull Plugin plugin) {
        super(plugin, "bench_saw", Material.STONECUTTER,
                "block.bench_saw.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
