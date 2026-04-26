package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Item buffer (T-441 / 1.8.0). Behaves like a high-priority {@code CONSUMER}
 * with a vanilla container next to it: pulls items out of the network into
 * the adjacent buffer chest, smoothing burst inputs from a producer chain.
 *
 * <p>1.8.0 ships placement + node registration; the priority-aware drain
 * logic that turns a buffer into a true sink lands with the routing
 * rework in 1.8.1.
 */
public final class SapientiaItemBuffer extends LogisticsContentBlock {
    public SapientiaItemBuffer(@NotNull Plugin plugin) {
        super(plugin, "item_buffer", Material.BARREL,
                "block.item_buffer.name", ItemNodeType.CONSUMER, EnergyTier.LOW, 5);
    }
}
