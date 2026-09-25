package dev.brmz.sapientia.core.logistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
 * Default {@link ItemService} implementation. Holds the in-memory
 * {@link ItemNetworkGraph}; node changes are queued on {@link ItemNodeStore}
 * and written by the database thread. Filter rules are cached per filter id
 * and preloaded with their chunk.
 */
public final class ItemServiceImpl implements ItemService {

    private final ItemNetworkGraph graph;
    private final ItemNodeStore store;
    private final Map<UUID, List<ItemFilterRule>> filterRules = new ConcurrentHashMap<>();
    private Consumer<Runnable> database = Runnable::run;

    public ItemServiceImpl(@NotNull ItemNetworkGraph graph, @NotNull ItemNodeStore store) {
        this.graph = graph;
        this.store = store;
    }

    public @NotNull ItemNetworkGraph graph() {
        return graph;
    }

    public @NotNull ItemNodeStore store() {
        return store;
    }

    /** Runs filter rule writes through {@code database} (the database thread) instead of inline. */
    public void runDatabaseWorkOn(@NotNull Consumer<Runnable> database) {
        this.database = database;
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
        boolean transit = type == ItemNodeType.CABLE || type == ItemNodeType.JUNCTION;
        UUID id = transit ? ItemNetworkGraph.transitId(key) : UUID.randomUUID();
        SimpleItemNode node = new SimpleItemNode(id, key, type, tier, priority);
        graph.addNode(node);
        store.put(node);
        if (type == ItemNodeType.FILTER) {
            filterRules.put(node.nodeId(), List.of()); // a new filter has no rules yet
        }
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
        List<ItemFilterRule> copy = List.copyOf(rules);
        filterRules.put(filterNodeId, copy);
        database.accept(() -> store.replaceFilterRules(filterNodeId, copy));
    }

    @Override
    public @NotNull List<ItemFilterRule> getFilterRules(@NotNull UUID filterNodeId) {
        return filterRules.computeIfAbsent(filterNodeId, store::loadFilterRules);
    }

    @Override
    public void setRoutingPolicy(@NotNull UUID networkId, @NotNull ItemRoutingPolicy policy) {
        graph.setRoutingPolicy(networkId, policy);
    }

    /** Nodes of one chunk and the rules of its filters, read by the database thread. */
    public record Loaded(@NotNull List<SimpleItemNode> nodes, @NotNull Map<UUID, List<ItemFilterRule>> rules) {}

    /** Reads a chunk's nodes and filter rules. Runs on the database thread. */
    public @NotNull Loaded loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        List<SimpleItemNode> nodes = store.loadChunk(world, chunkX, chunkZ);
        Map<UUID, List<ItemFilterRule>> rules = new HashMap<>();
        for (SimpleItemNode node : nodes) {
            if (node.type() == ItemNodeType.FILTER) {
                rules.put(node.nodeId(), List.copyOf(store.loadFilterRules(node.nodeId())));
            }
        }
        return new Loaded(nodes, rules);
    }

    /** Loads and adds a chunk's nodes on the calling thread. */
    public void hydrateChunk(@NotNull String world, int chunkX, int chunkZ) {
        applyLoaded(loadChunk(world, chunkX, chunkZ));
    }

    /** Adds nodes read by the database thread. Nodes placed since the read win. */
    public void applyLoaded(@NotNull Loaded loaded) {
        for (SimpleItemNode node : loaded.nodes()) {
            graph.addNode(node);
        }
        for (Map.Entry<UUID, List<ItemFilterRule>> entry : loaded.rules().entrySet()) {
            filterRules.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    /** Removes a chunk's nodes from the live graph. Item nodes hold no unsaved state. */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        for (SimpleItemNode node : graph.removeChunk(world, chunkX, chunkZ)) {
            filterRules.remove(node.nodeId());
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
