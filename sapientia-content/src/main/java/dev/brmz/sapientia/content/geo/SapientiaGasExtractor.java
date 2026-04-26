package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * MV-tier gas extractor (T-433 / 1.7.0). Pulls gases from underground pockets
 * (chunk-local in 1.7.1; placement-only stub in 1.7.0). Placement registers it
 * as an MV CONSUMER on the energy graph so it can be wired into a network and
 * surveyed by the wrench.
 */
public final class SapientiaGasExtractor extends MachineEnergyBlock {
    public SapientiaGasExtractor(@NotNull Plugin plugin) {
        super(plugin, "gas_extractor", Material.BLAST_FURNACE,
                "block.gas_extractor.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
