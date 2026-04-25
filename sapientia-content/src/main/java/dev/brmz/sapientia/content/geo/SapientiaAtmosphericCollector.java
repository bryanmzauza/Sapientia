package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * MV-tier atmospheric collector (T-433 / 1.7.0). Slowly samples the world
 * atmosphere into nitrogen / argon / carbon_dioxide gas tanks. In 1.7.0 the
 * block is a placement stub; the kinetic atmosphere-sampling tick (with biome
 * weighting) lands in 1.7.1.
 */
public final class SapientiaAtmosphericCollector extends MachineEnergyBlock {
    public SapientiaAtmosphericCollector(@NotNull Plugin plugin) {
        super(plugin, "atmospheric_collector", Material.SCULK_CATALYST,
                "block.atmospheric_collector.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
