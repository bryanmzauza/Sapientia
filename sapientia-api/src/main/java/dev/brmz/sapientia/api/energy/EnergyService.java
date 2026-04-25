package dev.brmz.sapientia.api.energy;

import java.util.Optional;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Service that manages energy nodes and the networks they form. Exposed via
 * {@code SapientiaAPI#energy()}.
 *
 * <p>Implementations are owned by {@code sapientia-core}; addons consume this
 * interface from their {@code SapientiaBlock#onPlace}/{@code onBreak} hooks.
 */
public interface EnergyService {

    /**
     * Registers a new node located at the given block. Idempotent: re-registering
     * the same coordinates returns the previously stored node.
     */
    @NotNull EnergyNode addNode(
            @NotNull Block block,
            @NotNull EnergyNodeType type,
            @NotNull EnergyTier tier,
            long bufferMax);

    /** Removes the node at the given block, if any. */
    void removeNode(@NotNull Block block);

    /** Looks up the node at a block, if any. */
    @NotNull Optional<EnergyNode> nodeAt(@NotNull Block block);

    /** Looks up the network containing a given node, if any. */
    @NotNull Optional<EnergyNetwork> networkOf(@NotNull EnergyNode node);
}
