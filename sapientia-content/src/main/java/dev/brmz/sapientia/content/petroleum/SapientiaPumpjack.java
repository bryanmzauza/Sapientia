package dev.brmz.sapientia.content.petroleum;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * MV-tier pumpjack (T-412 / 1.5.0). Geo-extracts {@code sapientia:crude_oil}
 * from the underlying chunk reservoir.
 *
 * <p>1.5.0 ships the block as an MV {@link EnergyNodeType#CONSUMER} stub: it
 * places, registers as an energy node, and opens the standard machine UI. The
 * full reservoir-depletion model (chunk-noise generation + per-chunk SQLite
 * persistence) is deferred to 1.5.1, see {@code docs/content-spec-T-41x.md}.
 */
public final class SapientiaPumpjack extends MachineEnergyBlock {
    public SapientiaPumpjack(@NotNull Plugin plugin) {
        super(plugin, "pumpjack", Material.BLAST_FURNACE,
                "block.pumpjack.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
