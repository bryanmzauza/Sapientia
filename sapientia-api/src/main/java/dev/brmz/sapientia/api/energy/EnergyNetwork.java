package dev.brmz.sapientia.api.energy;

import java.util.Collection;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

/**
 * A connected component of {@link EnergyNode}s. Networks are owned and managed by
 * the Sapientia core; addons receive immutable views via the public API. See
 * ROADMAP 0.3.0 (T-141).
 */
public interface EnergyNetwork {

    /** Stable id of this network for the lifetime of its connectivity. */
    @NotNull UUID networkId();

    /** Live view of the nodes currently in this network. Read-only. */
    @NotNull Collection<EnergyNode> nodes();

    /** Total {@code bufferCurrent} across all nodes. */
    long totalStored();

    /** Total {@code bufferMax} across all nodes. */
    long totalCapacity();

    /** Number of nodes in this network. */
    default int size() {
        return nodes().size();
    }
}
