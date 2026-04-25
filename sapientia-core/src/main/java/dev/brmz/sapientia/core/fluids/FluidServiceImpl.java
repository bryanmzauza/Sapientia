package dev.brmz.sapientia.core.fluids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * Default {@link FluidService} implementation (T-301 / 1.2.0). Holds the
 * in-memory {@link FluidNetworkGraph}, persists every mutation through
 * {@link FluidNodeStore}, and owns the {@link FluidType} registry.
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
        SimpleFluidNode node = new SimpleFluidNode(UUID.randomUUID(), key, type, tier, null, 0L);
        graph.addNode(node);
        store.put(node);
        return node;
    }

    @Override
    public void removeNode(@NotNull Block block) {
        BlockKey key = keyOf(block);
        if (graph.nodeAt(key) == null) return;
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

    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleFluidNode node : store.loadChunk(world, chunkX, chunkZ, types::get)) {
            graph.addNode(node);
        }
    }

    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        // Persist any dirty nodes first so contents survive reload.
        persistDirty();
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        java.util.List<BlockKey> victims = new ArrayList<>();
        for (SimpleFluidNode n : graph.nodes()) {
            BlockKey k = n.location();
            if (k.world().equals(world) && k.x() >= xMin && k.x() <= xMax && k.z() >= zMin && k.z() <= zMax) {
                victims.add(k);
            }
        }
        for (BlockKey k : victims) graph.removeNode(k);
    }

    /** Persists tank buffers that changed since the last call. */
    public void persistDirty() {
        for (SimpleFluidNode n : graph.nodes()) {
            if (n.takeDirty()) store.put(n);
        }
    }

    private static BlockKey keyOf(Block b) {
        return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    @SuppressWarnings("unused")
    private Logger logger() { return logger; }
}
