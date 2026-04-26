package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Unpackager (T-441 / 1.8.0; kinetic loop wired in T-450 / 1.8.1). Inverse of
 * {@link SapientiaPackager}: explodes a {@code packaged_bundle} back into its
 * component stacks into an adjacent container.
 *
 * <p>Registers as a {@link ItemNodeType#PRODUCER} node so the
 * {@code LogisticsTicker} (1.8.1) picks it up. NBT layout is locked by
 * ADR-020.
 */
public final class SapientiaUnpackager extends LogisticsContentBlock {

    public SapientiaUnpackager(@NotNull Plugin plugin) {
        super(plugin, "unpackager", Material.DISPENSER, "block.unpackager.name",
                ItemNodeType.PRODUCER, EnergyTier.LOW, 0);
    }
}
