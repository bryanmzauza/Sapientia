package dev.brmz.sapientia.api.logistics;

import java.util.Collection;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

/**
 * A connected component of {@link ItemNode}s. Networks are owned and managed by
 * the Sapientia core; addons receive immutable views via the public API.
 * See ROADMAP 1.1.0 (T-300).
 */
public interface ItemNetwork {

    /** Stable id of this network for the lifetime of its connectivity. */
    @NotNull UUID networkId();

    /** Live view of the nodes currently in this network. Read-only. */
    @NotNull Collection<ItemNode> nodes();

    /** Routing policy applied by the solver when distributing items. */
    @NotNull ItemRoutingPolicy routingPolicy();

    /** Number of nodes in this network. */
    default int size() {
        return nodes().size();
    }
}
