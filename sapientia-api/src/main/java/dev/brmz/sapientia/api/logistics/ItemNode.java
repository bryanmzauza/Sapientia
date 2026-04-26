package dev.brmz.sapientia.api.logistics;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A node participating in an item logistics {@code ItemNetworkGraph}
 * (T-300 / 1.1.0). Nodes are placed by Java content blocks; the implementation
 * is provided by Sapientia core, addons consume this read-only view.
 *
 * <p>Item buffers are <em>not</em> stored on the node itself: producers /
 * consumers operate on adjacent vanilla {@link org.bukkit.inventory.Inventory}
 * containers (chest, barrel, hopper, etc.), keeping the storage model identical
 * to vanilla hopper logic. See ROADMAP 1.1.0 and ADR-013.
 */
public interface ItemNode {

    /** Stable identifier of this node. */
    @NotNull UUID nodeId();

    @NotNull ItemNodeType type();

    /** Throughput tier of this node. Defaults to {@link EnergyTier#LOW}. */
    default @NotNull EnergyTier tier() {
        return EnergyTier.LOW;
    }

    /**
     * Higher values are served first when the network uses
     * {@link ItemRoutingPolicy#PRIORITY}. Defaults to {@code 0}.
     */
    default int priority() {
        return 0;
    }

    /** World block backing this node, if currently loaded. */
    @Nullable Block block();
}
