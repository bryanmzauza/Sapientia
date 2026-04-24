package dev.brmz.sapientia.api.energy;

import java.util.UUID;

/**
 * A node participating in an energy {@code NetworkGraph}. See docs/api-spec.md §2.2.
 * Implementations are provided by the Sapientia core; addons consume this interface.
 */
public interface EnergyNode {

    /** Stable identifier of this node. */
    UUID nodeId();

    EnergyNodeType type();

    /** Current energy held, in energy units (E). */
    long bufferCurrent();

    /** Maximum energy this node can hold, in energy units (E). */
    long bufferMax();

    /**
     * Flag this node as dirty, prompting the solver to include it on the next pass.
     * Safe to call from any thread; implementation must be thread-safe.
     */
    void markDirty();
}
