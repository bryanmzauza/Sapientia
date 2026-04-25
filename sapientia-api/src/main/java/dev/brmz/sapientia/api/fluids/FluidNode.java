package dev.brmz.sapientia.api.fluids;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A node in a {@code FluidNetworkGraph} (T-301 / 1.2.0). Tanks expose a buffer
 * (current contents + capacity); pumps/drains/pipes do not buffer fluid
 * themselves but interact with adjacent vanilla sources.
 */
public interface FluidNode {

    @NotNull UUID nodeId();

    @NotNull FluidNodeType type();

    default @NotNull EnergyTier tier() {
        return EnergyTier.LOW;
    }

    /** World block backing this node, if currently loaded. */
    @Nullable Block block();

    /**
     * Current contents of this node's buffer, or {@code null} when the node has
     * no internal storage (pipes, pumps, drains return {@code null}).
     */
    @Nullable FluidStack contents();

    /** Maximum buffer in mB. {@code 0} for non-tank nodes. */
    long capacityMb();
}
