package dev.brmz.sapientia.core.network;

import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.engine.BlockPositions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class of network node objects (energy, items, fluids).
 *
 * <p>Inside a {@link NodeGraph} only working nodes (generators, capacitors,
 * consumers, producers, filters, tanks, pumps, drains) stay objects, because
 * they hold state and are what solvers and machines touch. Transit nodes
 * (cables, pipes, junctions) are stored as primitive entries; asking the graph
 * for one returns a detached view.
 */
public abstract class GraphNode {

    private final BlockKey location;
    final long packed;

    volatile @Nullable NodeGroup<?> group;
    volatile @Nullable NodeGraph<?, ?> owner;
    int id = -1;
    int roleSlot = -1;

    private volatile boolean active = true;
    private volatile boolean dirty;

    protected GraphNode(@NotNull BlockKey location) {
        this.location = location;
        this.packed = BlockPositions.pack(location.x(), location.y(), location.z());
    }

    public final @NotNull BlockKey location() {
        return location;
    }

    /** Whether the node's chunk is within a player's activity radius. Solvers skip inactive nodes. */
    public final boolean isActive() {
        return active;
    }

    final void setActive(boolean active) {
        this.active = active;
    }

    /** Flags the node for the next persistence flush. Safe from any thread. */
    public final void markDirty() {
        if (dirty) return;
        dirty = true;
        NodeGraph<?, ?> graph = owner;
        if (graph != null) {
            graph.dirtyQueue.add(this);
        }
    }

    /** Clears and returns the unsaved-changes flag. */
    public final boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    /**
     * Records a change made from outside the network solver (a machine drew
     * energy, a ticker filled a tank): the node is persisted and its network
     * wakes up on the next solver cycle.
     */
    protected final void changedExternally() {
        markDirty();
        NodeGroup<?> g = group;
        if (g != null) {
            g.wake();
        }
    }
}
