package dev.brmz.sapientia.core.fluids;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidSpecs;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Concrete in-memory {@link FluidNode} (T-301 / 1.2.0). TANK nodes carry a
 * mutable {@link FluidStack}; non-tank roles ignore the buffer and report
 * {@code null} contents / {@code 0} capacity.
 */
public final class SimpleFluidNode implements FluidNode {

    private final UUID nodeId;
    private final BlockKey location;
    private final FluidNodeType type;
    private final EnergyTier tier;
    private final long capacityMb;

    private @Nullable FluidType heldType;
    private long amountMb;
    private volatile boolean dirty;

    public SimpleFluidNode(@NotNull UUID nodeId, @NotNull BlockKey location,
                           @NotNull FluidNodeType type, @NotNull EnergyTier tier,
                           @Nullable FluidType heldType, long amountMb) {
        this.nodeId = nodeId;
        this.location = location;
        this.type = type;
        this.tier = tier;
        this.capacityMb = type == FluidNodeType.TANK ? FluidSpecs.capacityMb(tier) : 0L;
        this.heldType = heldType;
        this.amountMb = Math.max(0L, Math.min(amountMb, capacityMb));
    }

    @Override public @NotNull UUID nodeId() { return nodeId; }
    @Override public @NotNull FluidNodeType type() { return type; }
    @Override public @NotNull EnergyTier tier() { return tier; }
    @Override public long capacityMb() { return capacityMb; }

    public @NotNull BlockKey location() { return location; }

    @Override
    public @Nullable Block block() {
        World world = Bukkit.getWorld(location.world());
        if (world == null) return null;
        return world.getBlockAt(location.x(), location.y(), location.z());
    }

    @Override
    public @Nullable FluidStack contents() {
        if (capacityMb <= 0L || heldType == null || amountMb <= 0L) return null;
        return new FluidStack(heldType, amountMb);
    }

    /**
     * Attempts to insert {@code amount} mB of {@code type}. Returns the amount
     * actually inserted (0 if the tank already holds a different fluid or is full).
     */
    public long offer(@NotNull FluidType type, long amount) {
        if (capacityMb <= 0L || amount <= 0L) return 0L;
        if (heldType != null && !heldType.id().equals(type.id())) return 0L;
        long room = capacityMb - amountMb;
        long take = Math.min(room, amount);
        if (take <= 0L) return 0L;
        heldType = type;
        amountMb += take;
        dirty = true;
        return take;
    }

    /**
     * Attempts to draw up to {@code amount} mB; returns the amount actually drawn.
     * If the tank empties as a result, the fluid type is cleared.
     */
    public long draw(long amount) {
        if (capacityMb <= 0L || amount <= 0L || heldType == null) return 0L;
        long give = Math.min(amount, amountMb);
        amountMb -= give;
        dirty = true;
        if (amountMb <= 0L) {
            amountMb = 0L;
            heldType = null;
        }
        return give;
    }

    public boolean takeDirty() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }

    public void markDirty() {
        dirty = true;
    }
}
