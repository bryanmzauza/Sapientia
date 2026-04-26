package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Item splitter (T-441 / 1.8.0). Distributes items between its outgoing
 * neighbours by ratio; in 1.8.0 the splitter registers as a {@code JUNCTION}
 * and the actual ratio table lands with the routing rework in 1.8.1.
 */
public final class SapientiaItemSplitter extends LogisticsContentBlock {
    public SapientiaItemSplitter(@NotNull Plugin plugin) {
        super(plugin, "item_splitter", Material.OBSERVER,
                "block.item_splitter.name", ItemNodeType.JUNCTION, EnergyTier.LOW, 0);
    }
}
