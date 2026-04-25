package dev.brmz.sapientia.core.logistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.api.logistics.ItemService;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link ItemService} implementation (T-300 / 1.1.0). Holds the
 * in-memory {@link ItemNetworkGraph} and persists every mutation through
 * {@link ItemNodeStore}. Filter rules are cached in-memory per filter id
 * for fast lookup by the solver.
 */
public final class ItemServiceImpl implements ItemService {

    private final ItemNetworkGraph graph;
    private final ItemNodeStore store;
    private final Map<UUID, List<ItemFilterRule>> filterRules = new ConcurrentHashMap<>();

    public ItemServiceImpl(@NotNull ItemNetworkGraph graph, @NotNull ItemNodeStore store) {
        this.graph = graph;
        this.store = store;
    }

    public @NotNull ItemNetworkGraph graph() {
        return graph;
    }

    @Override
    public @NotNull ItemNode addNode(
            @NotNull Block block, @NotNull ItemNodeType type,
            @NotNull EnergyTier tier, int priority) {
        BlockKey key = keyOf(block);
        SimpleItemNode existing = graph.nodeAt(key);
        if (existing != null) {
            return existing;
        }
        SimpleItemNode node = new SimpleItemNode(UUID.randomUUID(), key, type, tier, priority);
        graph.addNode(node);
        store.put(node);
        return node;
    }

    @Override
    public void removeNode(@NotNull Block block) {
        BlockKey key = keyOf(block);
        SimpleItemNode existing = graph.nodeAt(key);
        if (existing == null) return;
        graph.removeNode(key);
        store.delete(key);
        filterRules.remove(existing.nodeId());
    }

    @Override
    public @NotNull Optional<ItemNode> nodeAt(@NotNull Block block) {
        return Optional.ofNullable(graph.nodeAt(keyOf(block)));
    }

    @Override
    public @NotNull Optional<ItemNetwork> networkOf(@NotNull ItemNode node) {
        return Optional.ofNullable(graph.networkOf(node));
    }

    @Override
    public void setFilterRules(@NotNull UUID filterNodeId, @NotNull List<ItemFilterRule> rules) {
        filterRules.put(filterNodeId, List.copyOf(rules));
        store.replaceFilterRules(filterNodeId, rules);
    }

    @Override
    public @NotNull List<ItemFilterRule> getFilterRules(@NotNull UUID filterNodeId) {
        return filterRules.computeIfAbsent(filterNodeId, store::loadFilterRules);
    }

    @Override
    public void setRoutingPolicy(@NotNull UUID networkId, @NotNull ItemRoutingPolicy policy) {
        graph.setRoutingPolicy(networkId, policy);
    }

    /** Hydrates persisted nodes for a freshly loaded chunk into the live graph. */
    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleItemNode node : store.loadChunk(world, chunkX, chunkZ)) {
            graph.addNode(node);
        }
    }

    /** Removes every node belonging to a chunk from the live graph (no DB writes). */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        java.util.List<BlockKey> victims = new java.util.ArrayList<>();
        for (SimpleItemNode n : graph.nodes()) {
            BlockKey k = n.location();
            if (k.world().equals(world) && k.x() >= xMin && k.x() <= xMax && k.z() >= zMin && k.z() <= zMax) {
                victims.add(k);
            }
        }
        for (BlockKey k : victims) {
            SimpleItemNode n = graph.nodeAt(k);
            if (n != null) {
                filterRules.remove(n.nodeId());
            }
            graph.removeNode(k);
        }
    }

    private static BlockKey keyOf(Block b) {
        return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    /** Test seam: invalidate cached filter rules so the next read hits the store. */
    public void invalidateFilterCache() {
        filterRules.clear();
    }

    @SuppressWarnings("unused")
    private Map<UUID, List<ItemFilterRule>> rulesView() {
        return new HashMap<>(filterRules);
    }
}
