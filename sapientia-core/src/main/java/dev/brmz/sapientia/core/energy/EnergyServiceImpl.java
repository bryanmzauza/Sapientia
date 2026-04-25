package dev.brmz.sapientia.core.energy;

import java.util.Optional;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyService;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link EnergyService} implementation. Holds the in-memory
 * {@link NetworkGraph} and persists every mutation through {@link EnergyNodeStore}.
 */
public final class EnergyServiceImpl implements EnergyService {

    private final NetworkGraph graph;
    private final EnergyNodeStore store;

    public EnergyServiceImpl(@NotNull NetworkGraph graph, @NotNull EnergyNodeStore store) {
        this.graph = graph;
        this.store = store;
    }

    public @NotNull NetworkGraph graph() {
        return graph;
    }

    @Override
    public @NotNull EnergyNode addNode(
            @NotNull Block block, @NotNull EnergyNodeType type,
            @NotNull EnergyTier tier, long bufferMax) {
        BlockKey key = keyOf(block);
        SimpleEnergyNode existing = graph.nodeAt(key);
        if (existing != null) {
            return existing;
        }
        SimpleEnergyNode node = new SimpleEnergyNode(
                UUID.randomUUID(), key, type, tier, 0L, bufferMax);
        graph.addNode(node);
        store.put(node);
        return node;
    }

    @Override
    public void removeNode(@NotNull Block block) {
        BlockKey key = keyOf(block);
        SimpleEnergyNode existing = graph.nodeAt(key);
        if (existing == null) return;
        graph.removeNode(key);
        store.delete(key);
    }

    @Override
    public @NotNull Optional<EnergyNode> nodeAt(@NotNull Block block) {
        return Optional.ofNullable(graph.nodeAt(keyOf(block)));
    }

    @Override
    public @NotNull Optional<EnergyNetwork> networkOf(@NotNull EnergyNode node) {
        return Optional.ofNullable(graph.networkOf(node));
    }

    /** Hydrates persisted nodes for a freshly loaded chunk into the live graph. */
    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleEnergyNode node : store.loadChunk(world, chunkX, chunkZ)) {
            graph.addNode(node);
        }
    }

    /** Removes every node belonging to a chunk from the live graph (no DB writes). */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        // Snapshot and remove (avoid CME)
        java.util.List<BlockKey> victims = new java.util.ArrayList<>();
        for (SimpleEnergyNode n : graph.nodes()) {
            BlockKey k = n.location();
            if (k.world().equals(world) && k.x() >= xMin && k.x() <= xMax && k.z() >= zMin && k.z() <= zMax) {
                victims.add(k);
            }
        }
        for (BlockKey k : victims) {
            graph.removeNode(k);
        }
    }

    /** Persists every dirty node. Called on shutdown / periodic flush. */
    public void persistDirty() {
        for (SimpleEnergyNode n : graph.nodes()) {
            if (n.takeDirty()) {
                store.put(n);
            }
        }
    }

    private static BlockKey keyOf(Block b) {
        return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }
}
