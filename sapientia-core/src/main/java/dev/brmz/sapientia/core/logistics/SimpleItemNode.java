package dev.brmz.sapientia.core.logistics;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.GraphNode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Concrete {@link ItemNode}. Item buffers live in adjacent vanilla containers,
 * not on this object: the node only carries type, tier, priority and a stable id.
 */
public final class SimpleItemNode extends GraphNode implements ItemNode {

    private final UUID nodeId;
    private final ItemNodeType type;
    private final EnergyTier tier;
    private final int priority;

    public SimpleItemNode(
            @NotNull UUID nodeId,
            @NotNull BlockKey location,
            @NotNull ItemNodeType type,
            @NotNull EnergyTier tier,
            int priority) {
        super(location);
        this.nodeId = nodeId;
        this.type = type;
        this.tier = tier;
        this.priority = priority;
    }

    @Override
    public @NotNull UUID nodeId() {
        return nodeId;
    }

    @Override
    public @NotNull ItemNodeType type() {
        return type;
    }

    @Override
    public @NotNull EnergyTier tier() {
        return tier;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public @Nullable Block block() {
        World world = Bukkit.getWorld(location().world());
        if (world == null) {
            return null;
        }
        return world.getBlockAt(location().x(), location().y(), location().z());
    }
}
