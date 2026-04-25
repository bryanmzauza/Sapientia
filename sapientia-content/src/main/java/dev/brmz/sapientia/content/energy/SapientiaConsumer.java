package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier consumer (drains energy) (T-143). */
public final class SapientiaConsumer extends EnergyContentBlock {
    public SapientiaConsumer(@NotNull Plugin plugin) {
        super(plugin, "consumer", Material.REDSTONE_LAMP,
                "block.consumer.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
