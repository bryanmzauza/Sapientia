package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier capacitor (T-425 / 1.6.0). 16x the capacity of {@code capacitor_t1}. */
public final class SapientiaCapacitorT3 extends EnergyContentBlock {
    public SapientiaCapacitorT3(@NotNull Plugin plugin) {
        super(plugin, "capacitor_t3", Material.BEACON,
                "block.capacitor_t3.name", EnergyNodeType.CAPACITOR, EnergyTier.HIGH);
    }
}
