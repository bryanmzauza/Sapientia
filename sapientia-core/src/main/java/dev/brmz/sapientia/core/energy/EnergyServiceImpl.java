package dev.brmz.sapientia.core.energy;

import java.util.List;
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
 * {@link NetworkGraph}; every change is queued on {@link EnergyNodeStore} and
 * written by the database thread.
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

    public @NotNull EnergyNodeStore store() {
        return store;
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
        UUID id = type == EnergyNodeType.CABLE ? NetworkGraph.transitId(key) : UUID.randomUUID();
        SimpleEnergyNode node = new SimpleEnergyNode(id, key, type, tier, 0L, bufferMax);
        graph.addNode(node);
        store.put(node);
        return node;
    }

    @Override
    public void removeNode(@NotNull Block block) {
        BlockKey key = keyOf(block);
        if (!graph.contains(key)) return;
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

    /** Loads and adds a chunk's nodes on the calling thread. */
    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        applyLoaded(store.loadChunk(world, chunkX, chunkZ));
    }

    /** Adds nodes read by the database thread. Nodes placed since the read win. */
    public void applyLoaded(@NotNull List<SimpleEnergyNode> nodes) {
        for (SimpleEnergyNode node : nodes) {
            graph.addNode(node);
        }
    }

    /** Removes a chunk's nodes from the live graph, queueing unsaved buffers first. */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleEnergyNode node : graph.removeChunk(world, chunkX, chunkZ)) {
            if (node.takeDirty()) {
                store.put(node);
            }
        }
    }

    /** Queues every node changed since the last call. Costs the changed nodes, not all nodes. */
    public void persistDirty() {
        graph.drainDirty(store::put);
    }

    private static BlockKey keyOf(Block b) {
        return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }
}
