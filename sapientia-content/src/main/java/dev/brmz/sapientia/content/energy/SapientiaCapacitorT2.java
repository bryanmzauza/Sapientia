package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier capacitor (T-406 / 1.4.0). 4× the buffer of {@code capacitor_t1}. */
public final class SapientiaCapacitorT2 extends EnergyContentBlock {
    public SapientiaCapacitorT2(@NotNull Plugin plugin) {
        super(plugin, "capacitor_t2", Material.OXIDIZED_COPPER,
                "block.capacitor_t2.name", EnergyNodeType.CAPACITOR, EnergyTier.MID);
    }
}
