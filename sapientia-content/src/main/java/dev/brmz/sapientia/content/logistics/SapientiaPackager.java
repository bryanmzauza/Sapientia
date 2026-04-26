package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Packager (T-441 / 1.8.0; kinetic loop wired in T-450 / 1.8.1). Bundles the
 * items in an adjacent container into a single {@code packaged_bundle}
 * ItemStack with a Sapientia NBT payload, so a fixed recipe pattern (e.g.
 * solar-panel kit) ships as one stack through the logistics network.
 *
 * <p>Registers as a {@link ItemNodeType#CONSUMER} node so the
 * {@code LogisticsTicker} (1.8.1) picks it up: per cycle it pulls one stack
 * from the chest above, wraps it as a Sapientia bundle and fires
 * {@code SapientiaItemPackagedEvent} so addons can veto or rewrite the
 * bundle. NBT format is locked by ADR-020.
 *
 * <p>Companion: {@link SapientiaUnpackager}.
 */
public final class SapientiaPackager extends LogisticsContentBlock {

    public SapientiaPackager(@NotNull Plugin plugin) {
        super(plugin, "packager", Material.DROPPER, "block.packager.name",
                ItemNodeType.CONSUMER, EnergyTier.LOW, 0);
    }
}
