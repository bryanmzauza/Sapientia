package dev.brmz.sapientia.core.fluids;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidSpecs;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.GraphNode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Concrete {@link FluidNode}. TANK nodes carry a mutable {@link FluidStack};
 * other roles report {@code null} contents and {@code 0} capacity. Main thread only.
 *
 * <p>{@link #offer} and {@link #draw} are for everything outside the fluid
 * solver: they also wake the node's network. The solver uses {@link #fill}
 * and {@link #drain}, which do not.
 */
public final class SimpleFluidNode extends GraphNode implements FluidNode {

    private final UUID nodeId;
    private final FluidNodeType type;
    private final EnergyTier tier;
    private final long capacityMb;

    private @Nullable FluidType heldType;
    private long amountMb;

    public SimpleFluidNode(@NotNull UUID nodeId, @NotNull BlockKey location,
                           @NotNull FluidNodeType type, @NotNull EnergyTier tier,
                           @Nullable FluidType heldType, long amountMb) {
        super(location);
        this.nodeId = nodeId;
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

    @Override
    public @Nullable Block block() {
        World world = Bukkit.getWorld(location().world());
        if (world == null) return null;
        return world.getBlockAt(location().x(), location().y(), location().z());
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
        long inserted = fill(type, amount);
        if (inserted > 0L) changedExternally();
        return inserted;
    }

    /**
     * Attempts to draw up to {@code amount} mB; returns the amount actually drawn.
     * If the tank empties as a result, the fluid type is cleared.
     */
    public long draw(long amount) {
        long drawn = drain(amount);
        if (drawn > 0L) changedExternally();
        return drawn;
    }

    /** Solver-side {@link #offer}: persists the change without waking the network. */
    long fill(@NotNull FluidType type, long amount) {
        if (capacityMb <= 0L || amount <= 0L) return 0L;
        if (heldType != null && !heldType.id().equals(type.id())) return 0L;
        long take = Math.min(capacityMb - amountMb, amount);
        if (take <= 0L) return 0L;
        heldType = type;
        amountMb += take;
        markDirty();
        return take;
    }

    /** Solver-side {@link #draw}: persists the change without waking the network. */
    long drain(long amount) {
        if (capacityMb <= 0L || amount <= 0L || heldType == null) return 0L;
        long give = Math.min(amount, amountMb);
        amountMb -= give;
        markDirty();
        if (amountMb <= 0L) {
            amountMb = 0L;
            heldType = null;
        }
        return give;
    }
}
