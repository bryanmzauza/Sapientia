package dev.brmz.sapientia.api.logistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Service that manages item logistics nodes and the networks they form.
 * Exposed via {@code SapientiaAPI#logistics()} (T-300 / 1.1.0).
 *
 * <p>Implementations are owned by {@code sapientia-core}; addons consume this
 * interface from their {@code SapientiaBlock#onPlace}/{@code onBreak} hooks
 * exactly as they do for the energy system.
 */
public interface ItemService {

    /**
     * Registers a new node located at the given block. Idempotent: re-registering
     * the same coordinates returns the previously stored node.
     */
    @NotNull ItemNode addNode(
            @NotNull Block block,
            @NotNull ItemNodeType type,
            @NotNull EnergyTier tier,
            int priority);

    /** Removes the node at the given block, if any. */
    void removeNode(@NotNull Block block);

    /** Looks up the node at a block, if any. */
    @NotNull Optional<ItemNode> nodeAt(@NotNull Block block);

    /** Looks up the network containing a given node, if any. */
    @NotNull Optional<ItemNetwork> networkOf(@NotNull ItemNode node);

    /** Replaces the rules of a {@code FILTER} node. Empty list = pass everything. */
    void setFilterRules(@NotNull UUID filterNodeId, @NotNull List<ItemFilterRule> rules);

    /** Returns the rule list of a {@code FILTER} node (empty if none / not a filter). */
    @NotNull List<ItemFilterRule> getFilterRules(@NotNull UUID filterNodeId);

    /** Sets the routing policy for a network. */
    void setRoutingPolicy(@NotNull UUID networkId, @NotNull ItemRoutingPolicy policy);
}
