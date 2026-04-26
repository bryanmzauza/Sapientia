package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Demo LOW-tier item cable connecting logistics nodes (T-300 / 1.1.0). */
public final class SapientiaItemCable extends LogisticsContentBlock {
    public SapientiaItemCable(@NotNull Plugin plugin) {
        super(plugin, "item_cable", Material.IRON_BARS,
                "block.item_cable.name", ItemNodeType.CABLE, EnergyTier.LOW, 0);
    }
}
