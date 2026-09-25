package dev.brmz.sapientia.core.logistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import dev.brmz.sapientia.api.events.SapientiaItemFilterEvent;
import dev.brmz.sapientia.api.events.SapientiaItemFlowEvent;
import dev.brmz.sapientia.api.events.SapientiaItemRouteEvent;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.api.logistics.ItemSpecs;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Greedy item routing solver. Each cycle, for every network that is due:
 * <ol>
 *   <li>Every active {@code PRODUCER} extracts up to
 *       {@link ItemSpecs#throughputPerTick} items from its adjacent vanilla
 *       container.</li>
 *   <li>The batch passes the network's {@code FILTER}s
 *       ({@link SapientiaItemFilterEvent}) and is offered to consumers in the
 *       order set by the network's {@link ItemRoutingPolicy}.</li>
 *   <li>Anything no consumer accepts goes back to the source.</li>
 *   <li>A {@link SapientiaItemFlowEvent} reports the totals.</li>
 * </ol>
 *
 * <p>Runs on the main thread (it touches containers). A network where nothing
 * moved backs off exponentially, up to {@link #MAX_IDLE_CYCLES} cycles; one
 * without active producers or consumers sleeps until its topology changes or
 * one of its chunks becomes active.
 */
public final class ItemSolver {

    /** Longest back-off of an idle network, in cycles (the logistics cycle is one tick). */
    public static final int MAX_IDLE_CYCLES = 32;

    private final ItemNetworkGraph graph;
    private final ItemRegistry itemRegistry;
    private final Function<UUID, List<ItemFilterRule>> filterRulesProvider;
    private long cycle;

    public ItemSolver(
            @NotNull ItemNetworkGraph graph,
            @NotNull ItemRegistry itemRegistry,
            @NotNull Function<UUID, List<ItemFilterRule>> filterRulesProvider) {
        this.graph = graph;
        this.itemRegistry = itemRegistry;
        this.filterRulesProvider = filterRulesProvider;
    }

    public void tick() {
        long c = ++cycle;
        for (ItemNetworkGraph.Network network : graph.collectDue(c)) {
            if (network.isRemoved()) continue; // an event listener changed the topology
            switch (tickNetwork(network)) {
                case WORKED -> graph.worked(network, c);
                case IDLE -> graph.idle(network, c, MAX_IDLE_CYCLES);
                case DORMANT -> graph.idle(network, c, 0);
            }
        }
    }

    private enum Outcome { WORKED, IDLE, DORMANT }

    private Outcome tickNetwork(ItemNetworkGraph.Network network) {
        List<SimpleItemNode> producers = active(network.role(ItemNetworkGraph.PRODUCERS));
        List<SimpleItemNode> consumers = active(network.consumersById());
        if (producers.isEmpty() || consumers.isEmpty()) {
            return Outcome.DORMANT;
        }
        List<SimpleItemNode> filters = active(network.role(ItemNetworkGraph.FILTERS));
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
            produced += batch.getAmount();

            boolean blocked = false;
            for (SimpleItemNode filter : filters) {
                if (!evaluateFilter(filter.nodeId(), batch)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                AdjacentContainers.insertInto(source, batch);
                continue;
            }

            int routed = routeToConsumers(network, producer, consumers, batch, policy);
            consumed += routed;
            int leftover = batch.getAmount();
            if (leftover > 0) {
                // Best-effort return to source; whatever even the source cannot take is in transit.
                int returnedQty = AdjacentContainers.insertInto(source, batch);
                inTransit += (leftover - returnedQty);
            }
        }

        if (produced > 0) {
            Bukkit.getPluginManager().callEvent(
                    new SapientiaItemFlowEvent(network, produced, consumed, inTransit));
        }
        return consumed > 0 ? Outcome.WORKED : Outcome.IDLE;
    }

    private static List<SimpleItemNode> active(List<SimpleItemNode> nodes) {
        List<SimpleItemNode> out = new ArrayList<>(nodes.size());
        for (SimpleItemNode n : nodes) {
            if (n.isActive()) out.add(n);
        }
        return out;
    }

    private boolean evaluateFilter(@NotNull UUID filterNodeId, @NotNull ItemStack stack) {
        List<ItemFilterRule> rules = filterRulesProvider.apply(filterNodeId);
        boolean allowed = ItemFilterRuleMatcher.allows(rules, itemRegistry, stack);
        SapientiaItemFilterEvent event = new SapientiaItemFilterEvent(filterNodeId, stack, allowed);
        Bukkit.getPluginManager().callEvent(event);
        return event.isAllowed();
    }

    private int routeToConsumers(
            @NotNull ItemNetworkGraph.Network network,
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
            }
        }
        return totalInserted;
    }

    private List<SimpleItemNode> orderConsumers(
            @NotNull List<SimpleItemNode> consumers,
            @NotNull ItemNetworkGraph.Network network,
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
                int cursor = network.roundRobinCursor % consumers.size();
                network.roundRobinCursor = (cursor + 1) % consumers.size();
                List<SimpleItemNode> rotated = new ArrayList<>(consumers.size());
                for (int i = 0; i < consumers.size(); i++) {
                    rotated.add(consumers.get((cursor + i) % consumers.size()));
                }
                yield rotated;
            }
        };
    }
}
