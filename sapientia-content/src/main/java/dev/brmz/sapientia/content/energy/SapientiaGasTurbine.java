package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * HV-tier gas turbine (T-425 / 1.6.0). Burns hydrogen / ethylene at high SU/mB
 * yield. Kinetic loop wiring lands in 1.6.1.
 */
public final class SapientiaGasTurbine extends EnergyContentBlock {
    public SapientiaGasTurbine(@NotNull Plugin plugin) {
        super(plugin, "gas_turbine", Material.BLAST_FURNACE,
                "block.gas_turbine.name", EnergyNodeType.GENERATOR, EnergyTier.HIGH);
    }
}
