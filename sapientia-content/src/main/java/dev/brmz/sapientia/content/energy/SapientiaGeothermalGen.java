package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * HV-tier geothermal generator (T-425 / 1.6.0). Consumes lava (or hot fluid)
 * from a tank below; emits HV-tier energy. Kinetic loop wiring lands in 1.6.1.
 */
public final class SapientiaGeothermalGen extends EnergyContentBlock {
    public SapientiaGeothermalGen(@NotNull Plugin plugin) {
        super(plugin, "geothermal_gen", Material.MAGMA_BLOCK,
                "block.geothermal_gen.name", EnergyNodeType.GENERATOR, EnergyTier.HIGH);
    }
}
