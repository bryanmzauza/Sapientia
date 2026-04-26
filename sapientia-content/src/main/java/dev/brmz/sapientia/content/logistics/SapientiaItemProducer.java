package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Demo LOW-tier item producer (T-300 / 1.1.0). Pulls items from an adjacent
 * vanilla container (chest, barrel, hopper, etc.) into the logistics network.
 */
public final class SapientiaItemProducer extends LogisticsContentBlock {
    public SapientiaItemProducer(@NotNull Plugin plugin) {
        super(plugin, "item_producer", Material.DROPPER,
                "block.item_producer.name", ItemNodeType.PRODUCER, EnergyTier.LOW, 0);
    }
}
