package dev.brmz.sapientia.core.logistics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.network.NodeGraph;
import dev.brmz.sapientia.core.network.NodeGroup;
import org.jetbrains.annotations.NotNull;

/**
 * Item logistics networks: {@link SimpleItemNode}s grouped into connected
 * {@link ItemNetwork}s. Producers, consumers and filters are objects tracked
 * per network, so the solver never walks cables or junctions; those are
 * primitive entries. A network's routing policy survives splits.
 */
public final class ItemNetworkGraph extends NodeGraph<SimpleItemNode, ItemNetworkGraph.Network> {

    static final int PRODUCERS = 0;
    static final int CONSUMERS = 1;
    static final int FILTERS = 2;
    private static final ItemNodeType[] TYPES = ItemNodeType.values();
    private static final EnergyTier[] TIERS = EnergyTier.values();

    public ItemNetworkGraph() {
        super(3, false);
    }

    @Override
    protected @NotNull Network newGroup() {
        return new Network();
    }

    @Override
    protected byte transitKind(@NotNull SimpleItemNode node) {
        return (byte) (node.type().ordinal() << 3 | node.tier().ordinal());
    }

    @Override
    protected @NotNull SimpleItemNode transitView(@NotNull BlockKey key, byte kind) {
        return new SimpleItemNode(transitId(key), key, TYPES[kind >> 3], TIERS[kind & 7], 0);
    }

    /** Stable id of the cable or junction at {@code key}; they are not stored as objects. */
    public static @NotNull UUID transitId(@NotNull BlockKey key) {
        return UUID.nameUUIDFromBytes(("sapientia:items:" + key.world() + ":" + key.x() + ":" + key.y() + ":" + key.z())
                .getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected int roleOf(@NotNull SimpleItemNode node) {
        return switch (node.type()) {
            case PRODUCER -> PRODUCERS;
            case CONSUMER -> CONSUMERS;
            case FILTER -> FILTERS;
            case CABLE, JUNCTION -> -1;
        };
    }

    @Override
    protected void inherit(@NotNull Network from, @NotNull Network into) {
        into.routingPolicy = from.routingPolicy;
    }

    public @NotNull Collection<ItemNetwork> networks() {
        return Collections.unmodifiableCollection(groups());
    }

    public ItemNetwork networkOf(@NotNull ItemNode node) {
        return node instanceof SimpleItemNode simple ? groupOf(simple) : null;
    }

    /** Updates the routing policy of a network in place. */
    public void setRoutingPolicy(@NotNull UUID networkId, @NotNull ItemRoutingPolicy policy) {
        for (Network n : groups()) {
            if (n.networkId().equals(networkId)) {
                n.routingPolicy = policy;
                n.wake();
                return;
            }
        }
    }

    public @NotNull Collection<SimpleItemNode> membersOf(@NotNull ItemNetwork network) {
        return network instanceof Network n && !n.isRemoved() ? n.members() : Collections.emptyList();
    }

    /** One item network. The public surface goes through {@link ItemNetwork}. */
    public static final class Network extends NodeGroup<SimpleItemNode> implements ItemNetwork {

        private static final Comparator<SimpleItemNode> BY_ID = Comparator.comparing(n -> n.nodeId().toString());

        private ItemRoutingPolicy routingPolicy = ItemRoutingPolicy.ROUND_ROBIN;
        int roundRobinCursor;
        private List<SimpleItemNode> sortedConsumers = List.of();
        private int sortedVersion = -1;

        Network() {
            super(3);
        }

        @Override
        public Collection<ItemNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members()));
        }

        @Override
        public ItemRoutingPolicy routingPolicy() {
            return routingPolicy;
        }

        /** Consumers in stable id order, re-sorted only after membership changes. */
        List<SimpleItemNode> consumersById() {
            if (sortedVersion != version()) {
                List<SimpleItemNode> sorted = new ArrayList<>(role(CONSUMERS));
                sorted.sort(BY_ID);
                sortedConsumers = sorted;
                sortedVersion = version();
            }
            return sortedConsumers;
        }
    }
}
