package dev.brmz.sapientia.core.logistics;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable concrete implementation of {@link ItemNode}. Item buffers live in
 * adjacent vanilla containers, not on this object — the node only carries
 * type, tier, priority and a stable id. See ROADMAP 1.1.0 (T-300).
 */
public final class SimpleItemNode implements ItemNode {

    private final UUID nodeId;
    private final BlockKey location;
    private final ItemNodeType type;
    private final EnergyTier tier;
    private final int priority;
    private volatile boolean dirty;

    public SimpleItemNode(
            @NotNull UUID nodeId,
            @NotNull BlockKey location,
            @NotNull ItemNodeType type,
            @NotNull EnergyTier tier,
            int priority) {
        this.nodeId = nodeId;
        this.location = location;
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
        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            return null;
        }
        return world.getBlockAt(location.x(), location.y(), location.z());
    }

    public @NotNull BlockKey location() {
        return location;
    }

    public boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public void markDirty() {
        this.dirty = true;
    }
}
