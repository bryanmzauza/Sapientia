package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Multi-pass filter chamber (T-441 / 1.8.0). Stronger relative to
 * {@link SapientiaItemFilter}: in 1.8.1 it will hold up to four chained
 * {@code ItemFilterRule} sets that the solver evaluates in order. In 1.8.0
 * it ships as a {@code FILTER} node so existing networks place and route
 * items through it identically to the 1.1.0 filter.
 */
public final class SapientiaFilterChamber extends LogisticsContentBlock {
    public SapientiaFilterChamber(@NotNull Plugin plugin) {
        super(plugin, "filter_chamber", Material.IRON_TRAPDOOR,
                "block.filter_chamber.name", ItemNodeType.FILTER, EnergyTier.LOW, 0);
    }
}
