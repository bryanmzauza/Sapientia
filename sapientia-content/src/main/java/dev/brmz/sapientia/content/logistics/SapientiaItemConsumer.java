package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Demo LOW-tier item consumer (T-300 / 1.1.0). Pushes items received from the
 * network into an adjacent vanilla container.
 */
public final class SapientiaItemConsumer extends LogisticsContentBlock {
    public SapientiaItemConsumer(@NotNull Plugin plugin) {
        super(plugin, "item_consumer", Material.HOPPER,
                "block.item_consumer.name", ItemNodeType.CONSUMER, EnergyTier.LOW, 0);
    }
}
