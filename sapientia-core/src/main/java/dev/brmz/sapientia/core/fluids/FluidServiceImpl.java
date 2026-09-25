package dev.brmz.sapientia.core.fluids;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNetwork;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidService;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link FluidService} implementation. Holds the in-memory
 * {@link FluidNetworkGraph} and owns the {@link FluidType} registry; changes
 * are queued on {@link FluidNodeStore} and written by the database thread.
 */
public final class FluidServiceImpl implements FluidService {

    private final FluidNetworkGraph graph;
    private final FluidNodeStore store;
    private final Map<NamespacedKey, FluidType> types = new ConcurrentHashMap<>();
    private final Logger logger;

    public FluidServiceImpl(@NotNull Logger logger,
                            @NotNull FluidNetworkGraph graph,
                            @NotNull FluidNodeStore store) {
        this.logger = logger;
        this.graph = graph;
        this.store = store;
    }

    public @NotNull FluidNetworkGraph graph() {
        return graph;
    }

    public @NotNull FluidNodeStore store() {
        return store;
    }

    @Override
    public void registerType(@NotNull FluidType type) {
        types.putIfAbsent(type.id(), type);
    }

    @Override
    public @NotNull Optional<FluidType> type(@NotNull NamespacedKey id) {
        return Optional.ofNullable(types.get(id));
    }

    @Override
    public @NotNull Collection<FluidType> types() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(types).values());
    }

    @Override
    public @NotNull FluidNode addNode(@NotNull Block block, @NotNull FluidNodeType type,
                                      @NotNull EnergyTier tier) {
        BlockKey key = keyOf(block);
        SimpleFluidNode existing = graph.nodeAt(key);
        if (existing != null) return existing;
        boolean transit = type != FluidNodeType.TANK && type != FluidNodeType.PUMP && type != FluidNodeType.DRAIN;
        UUID id = transit ? FluidNetworkGraph.transitId(key) : UUID.randomUUID();
        SimpleFluidNode node = new SimpleFluidNode(id, key, type, tier, null, 0L);
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
    public @NotNull Optional<FluidNode> nodeAt(@NotNull Block block) {
        return Optional.ofNullable(graph.nodeAt(keyOf(block)));
    }

    @Override
    public @NotNull Optional<FluidNetwork> networkOf(@NotNull FluidNode node) {
        return Optional.ofNullable(graph.networkOf(node));
    }

    @Override
    public @NotNull Collection<FluidNetwork> networks() {
        return graph.networks();
    }

    /** Reads a chunk's nodes. Runs on the database thread. */
    public @NotNull List<SimpleFluidNode> loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        return store.loadChunk(world, chunkX, chunkZ, types::get);
    }

    /** Loads and adds a chunk's nodes on the calling thread. */
    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        applyLoaded(loadChunk(world, chunkX, chunkZ));
    }

    /** Adds nodes read by the database thread. Nodes placed since the read win. */
    public void applyLoaded(@NotNull List<SimpleFluidNode> nodes) {
        for (SimpleFluidNode node : nodes) {
            graph.addNode(node);
        }
    }

    /** Removes a chunk's nodes from the live graph, queueing unsaved tank contents first. */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleFluidNode node : graph.removeChunk(world, chunkX, chunkZ)) {
            if (node.takeDirty()) {
                store.put(node);
            }
        }
    }

    /** Queues every tank changed since the last call. Costs the changed tanks, not all nodes. */
    public void persistDirty() {
        graph.drainDirty(store::put);
    }

    private static BlockKey keyOf(Block b) {
        return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    @SuppressWarnings("unused")
    private Logger logger() { return logger; }
}
