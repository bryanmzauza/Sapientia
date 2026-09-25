package dev.brmz.sapientia.core.energy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.GraphNode;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete {@link EnergyNode}. The buffer is an {@link AtomicLong} because the
 * energy solver runs on its own thread while machines draw on the main thread.
 *
 * <p>{@link #offer} and {@link #draw} are for everything outside the solver:
 * they also wake the node's network. The solver uses {@link #fill} and
 * {@link #drain}, which do not, so a network with nothing to do stays asleep.
 */
public final class SimpleEnergyNode extends GraphNode implements EnergyNode {

    private final UUID nodeId;
    private final EnergyNodeType type;
    private final EnergyTier tier;
    private final AtomicLong bufferCurrent;
    private final long bufferMax;

    public SimpleEnergyNode(
            @NotNull UUID nodeId,
            @NotNull BlockKey location,
            @NotNull EnergyNodeType type,
            @NotNull EnergyTier tier,
            long bufferCurrent,
            long bufferMax) {
        super(location);
        this.nodeId = nodeId;
        this.type = type;
        this.tier = tier;
        this.bufferCurrent = new AtomicLong(bufferCurrent);
        this.bufferMax = bufferMax;
    }

    @Override
    public UUID nodeId() {
        return nodeId;
    }

    @Override
    public EnergyNodeType type() {
        return type;
    }

    @Override
    public EnergyTier tier() {
        return tier;
    }

    @Override
    public long bufferCurrent() {
        return bufferCurrent.get();
    }

    @Override
    public long bufferMax() {
        return bufferMax;
    }

    /** Adds energy up to {@link #bufferMax}; returns the amount actually inserted. */
    public long offer(long amount) {
        long inserted = fill(amount);
        if (inserted > 0) changedExternally();
        return inserted;
    }

    /** Removes up to {@code amount} energy; returns the amount actually drawn. */
    public long draw(long amount) {
        long drawn = drain(amount);
        if (drawn > 0) changedExternally();
        return drawn;
    }

    /** Solver-side {@link #offer}: persists the change without waking the network. */
    long fill(long amount) {
        if (amount <= 0) return 0;
        long curr;
        long inserted;
        do {
            curr = bufferCurrent.get();
            inserted = Math.min(amount, bufferMax - curr);
            if (inserted <= 0) return 0;
        } while (!bufferCurrent.compareAndSet(curr, curr + inserted));
        markDirty();
        return inserted;
    }

    /** Solver-side {@link #draw}: persists the change without waking the network. */
    long drain(long amount) {
        if (amount <= 0) return 0;
        long curr;
        long drawn;
        do {
            curr = bufferCurrent.get();
            drawn = Math.min(amount, curr);
            if (drawn <= 0) return 0;
        } while (!bufferCurrent.compareAndSet(curr, curr - drawn));
        markDirty();
        return drawn;
    }
}
