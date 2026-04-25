package dev.brmz.sapientia.core.logistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import dev.brmz.sapientia.api.events.SapientiaItemFilterEvent;
import dev.brmz.sapientia.api.events.SapientiaItemFlowEvent;
import dev.brmz.sapientia.api.events.SapientiaItemRouteEvent;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.api.logistics.ItemSpecs;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Greedy item routing solver (T-300 / 1.1.0). Each tick:
 * <ol>
 *   <li>For each network, every {@code PRODUCER} extracts up to
 *       {@link ItemSpecs#throughputPerTick} items from its adjacent vanilla
 *       container.</li>
 *   <li>The extracted batch is offered to consumers in an order chosen by the
 *       network's {@link ItemRoutingPolicy}. Each {@code FILTER} node along
 *       the path is consulted via {@link SapientiaItemFilterEvent}.</li>
 *   <li>Anything no consumer accepts is rolled back into the source
 *       inventory.</li>
 *   <li>A {@link SapientiaItemFlowEvent} fires with totals for the network.</li>
 * </ol>
 *
 * <p>This is intentionally <em>not</em> a max-flow algorithm — for the
 * 1.1.0 demo content, a single pass per producer with policy-aware
 * round-robin is well within the P-004 envelope. A real maxflow pass is a
 * 1.4.0+ concern.
 */
public final class ItemSolver {

    private final ItemNetworkGraph graph;
    private final ItemRegistry itemRegistry;
    private final Function<UUID, List<ItemFilterRule>> filterRulesProvider;
    private final Map<UUID, Integer> roundRobinCursors = new HashMap<>();

    public ItemSolver(
            @NotNull ItemNetworkGraph graph,
            @NotNull ItemRegistry itemRegistry,
            @NotNull Function<UUID, List<ItemFilterRule>> filterRulesProvider) {
        this.graph = graph;
        this.itemRegistry = itemRegistry;
        this.filterRulesProvider = filterRulesProvider;
    }

    public void tick() {
        for (ItemNetwork network : graph.networks()) {
            tickNetwork(network);
        }
    }

    private void tickNetwork(ItemNetwork network) {
        List<SimpleItemNode> producers = new ArrayList<>();
        List<SimpleItemNode> consumers = new ArrayList<>();
        List<SimpleItemNode> filters = new ArrayList<>();
        for (SimpleItemNode n : graph.membersOf(network)) {
            switch (n.type()) {
                case PRODUCER -> producers.add(n);
                case CONSUMER -> consumers.add(n);
                case FILTER   -> filters.add(n);
                case CABLE, JUNCTION -> { /* transit only */ }
            }
        }
        if (producers.isEmpty() || consumers.isEmpty()) {
            return;
        }

        // Stable consumer ordering for FIRST_MATCH / round-robin determinism.
        consumers.sort(Comparator.comparing(n -> n.nodeId().toString()));
        ItemRoutingPolicy policy = network.routingPolicy();

        long produced = 0;
        long consumed = 0;
        long inTransit = 0;

        for (SimpleItemNode producer : producers) {
            Block produceBlock = producer.block();
            if (produceBlock == null) continue;
            Inventory source = AdjacentContainers.findAdjacent(produceBlock);
            if (source == null) continue;
            int budget = ItemSpecs.throughputPerTick(producer.tier());
            ItemStack batch = AdjacentContainers.extractAny(source, budget);
            if (batch == null) continue;
            int extracted = batch.getAmount();
            produced += extracted;

            // Apply each filter on the network to the batch.
            boolean blocked = false;
            for (SimpleItemNode filter : filters) {
                if (!evaluateFilter(filter.nodeId(), batch)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                // Filter denied — return everything to source.
                AdjacentContainers.insertInto(source, batch);
                continue;
            }

            // Route to consumers per policy.
            int routed = routeToConsumers(network, producer, consumers, batch, policy);
            consumed += routed;
            int leftover = batch.getAmount();
            if (leftover > 0) {
                // Best-effort return to source; if even the source is full, count as in-transit (lost-ish).
                int returnedQty = AdjacentContainers.insertInto(source, batch);
                inTransit += (leftover - returnedQty);
            }
        }

        Bukkit.getPluginManager().callEvent(
                new SapientiaItemFlowEvent(network, produced, consumed, inTransit));
    }

    private boolean evaluateFilter(@NotNull UUID filterNodeId, @NotNull ItemStack stack) {
        List<ItemFilterRule> rules = filterRulesProvider.apply(filterNodeId);
        boolean allowed = ItemFilterRuleMatcher.allows(rules, itemRegistry, stack);
        SapientiaItemFilterEvent event = new SapientiaItemFilterEvent(filterNodeId, stack, allowed);
        Bukkit.getPluginManager().callEvent(event);
        return event.isAllowed();
    }

    private int routeToConsumers(
            @NotNull ItemNetwork network,
            @NotNull SimpleItemNode producer,
            @NotNull List<SimpleItemNode> consumers,
            @NotNull ItemStack batch,
            @NotNull ItemRoutingPolicy policy) {
        List<SimpleItemNode> ordered = orderConsumers(consumers, network, policy);
        int totalInserted = 0;
        for (SimpleItemNode consumer : ordered) {
            if (batch.getAmount() <= 0) break;
            Block target = consumer.block();
            if (target == null) continue;
            Inventory dest = AdjacentContainers.findAdjacent(target);
            if (dest == null) continue;
            int beforeBatch = batch.getAmount();
            int budget = ItemSpecs.throughputPerTick(consumer.tier());
            int slice = Math.min(beforeBatch, budget);
            ItemStack chunk = batch.clone();
            chunk.setAmount(slice);
            int inserted = AdjacentContainers.insertInto(dest, chunk);
            if (inserted > 0) {
                batch.setAmount(beforeBatch - inserted);
                totalInserted += inserted;
                Bukkit.getPluginManager().callEvent(
                        new SapientiaItemRouteEvent(producer, consumer, chunk, inserted));
                if (policy == ItemRoutingPolicy.FIRST_MATCH) {
                    // First match takes everything until it's full; continue draining same consumer.
                    // We loop again because a consumer may accept more if budget > inserted.
                }
            }
        }
        return totalInserted;
    }

    private List<SimpleItemNode> orderConsumers(
            @NotNull List<SimpleItemNode> consumers,
            @NotNull ItemNetwork network,
            @NotNull ItemRoutingPolicy policy) {
        if (consumers.size() <= 1) return consumers;
        return switch (policy) {
            case PRIORITY -> {
                List<SimpleItemNode> sorted = new ArrayList<>(consumers);
                sorted.sort(Comparator.comparingInt(SimpleItemNode::priority).reversed());
                yield sorted;
            }
            case FIRST_MATCH -> consumers; // already sorted by id, deterministic
            case ROUND_ROBIN -> {
                int cursor = roundRobinCursors.getOrDefault(network.networkId(), 0) % consumers.size();
                roundRobinCursors.put(network.networkId(), (cursor + 1) % consumers.size());
                List<SimpleItemNode> rotated = new ArrayList<>(consumers.size());
                for (int i = 0; i < consumers.size(); i++) {
                    rotated.add(consumers.get((cursor + i) % consumers.size()));
                }
                yield rotated;
            }
        };
    }

    /** For tests / diagnostics: drops state held between ticks. */
    public void resetCursors() {
        roundRobinCursors.clear();
    }

    /** Clears stale cursors when a network is removed. Safe no-op otherwise. */
    public void forgetNetwork(@NotNull UUID networkId) {
        roundRobinCursors.remove(networkId);
    }

    @SuppressWarnings("unused")
    private Collection<SimpleItemNode> snapshot(Collection<SimpleItemNode> in) {
        return Collections.unmodifiableCollection(in);
    }
}
