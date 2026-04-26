package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Overflow module (T-441 / 1.8.0). Routing safety valve: when downstream
 * consumers are saturated, the network re-routes items through the overflow
 * module's adjacent container. Registers as a low-priority {@code CONSUMER}
 * so the existing greedy solver only fills it once everyone else has refused.
 * Full priority semantics arrive with the routing rework in 1.8.1.
 */
public final class SapientiaOverflowModule extends LogisticsContentBlock {
    public SapientiaOverflowModule(@NotNull Plugin plugin) {
        super(plugin, "overflow_module", Material.HOPPER,
                "block.overflow_module.name", ItemNodeType.CONSUMER, EnergyTier.LOW, -10);
    }
}
