package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Conveyor belt (T-442 / 1.8.0). Visible item-on-belt rendering arrives in
 * 1.8.1 (display-entity API on Java; static texture on Bedrock). For 1.8.0
 * the conveyor belt is just another logistics junction so existing networks
 * can already route through it. Block sits on the rail material so it visually
 * reads like a belt today and will swap to its custom model in 1.8.1.
 */
public final class SapientiaConveyorBelt extends LogisticsContentBlock {
    public SapientiaConveyorBelt(@NotNull Plugin plugin) {
        super(plugin, "conveyor_belt", Material.RAIL,
                "block.conveyor_belt.name", ItemNodeType.JUNCTION, EnergyTier.LOW, 0);
    }
}
