package dev.brmz.sapientia.core.energy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * Mutable, thread-safe-ish concrete implementation of {@link EnergyNode}. Buffers
 * are stored in an {@link AtomicLong} so {@code markDirty} can be called from any
 * thread, but solver mutations happen on the main thread.
 */
public final class SimpleEnergyNode implements EnergyNode {

    private final UUID nodeId;
    private final BlockKey location;
    private final EnergyNodeType type;
    private final EnergyTier tier;
    private final AtomicLong bufferCurrent;
    private final long bufferMax;
    private volatile boolean dirty;

    public SimpleEnergyNode(
            @NotNull UUID nodeId,
            @NotNull BlockKey location,
            @NotNull EnergyNodeType type,
            @NotNull EnergyTier tier,
            long bufferCurrent,
            long bufferMax) {
        this.nodeId = nodeId;
        this.location = location;
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

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    public boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public @NotNull BlockKey location() {
        return location;
    }

    /** Adds energy up to {@link #bufferMax}; returns the amount actually inserted. */
    public long offer(long amount) {
        if (amount <= 0) return 0;
        long curr;
        long inserted;
        do {
            curr = bufferCurrent.get();
            inserted = Math.min(amount, bufferMax - curr);
            if (inserted <= 0) return 0;
        } while (!bufferCurrent.compareAndSet(curr, curr + inserted));
        dirty = true;
        return inserted;
    }

    /** Removes up to {@code amount} energy; returns the amount actually drawn. */
    public long draw(long amount) {
        if (amount <= 0) return 0;
        long curr;
        long drawn;
        do {
            curr = bufferCurrent.get();
            drawn = Math.min(amount, curr);
            if (drawn <= 0) return 0;
        } while (!bufferCurrent.compareAndSet(curr, curr - drawn));
        dirty = true;
        return drawn;
    }
}
