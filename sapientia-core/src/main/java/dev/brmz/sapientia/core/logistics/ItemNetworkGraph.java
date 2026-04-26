package dev.brmz.sapientia.core.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory adjacency graph of {@link SimpleItemNode}s grouped into connected
 * components ({@link ItemNetwork}s). Nodes are adjacent when their {@link BlockKey}s
 * differ by 1 along exactly one axis (6-neighborhood) and are in the same world.
 *
 * <p>Direct port of {@code NetworkGraph} (energy) for items. See ADR-013 — the
 * adjacency / split / merge logic is identical, only the node payload differs.
 * Mutations recompute affected networks lazily by BFS.
 *
 * <p>See ROADMAP 1.1.0 (T-300).
 */
public final class ItemNetworkGraph {

    private final Map<BlockKey, SimpleItemNode> nodesByKey = new HashMap<>();
    private final Map<UUID, Network> networksById = new LinkedHashMap<>();
    private final Map<UUID, Network> networkOfNode = new HashMap<>();

    /** Adds a node to the graph, merging or extending networks as needed. */
    public void addNode(@NotNull SimpleItemNode node) {
        BlockKey key = node.location();
        if (nodesByKey.putIfAbsent(key, node) != null) {
            return;
        }
        Set<Network> neighbours = neighbouringNetworks(key);
        Network host;
        if (neighbours.isEmpty()) {
            host = new Network();
            networksById.put(host.networkId, host);
        } else {
            host = neighbours.stream().max((a, b) -> Integer.compare(a.size(), b.size())).orElseThrow();
            for (Network other : neighbours) {
                if (other == host) continue;
                for (SimpleItemNode n : other.members) {
                    host.members.add(n);
                    networkOfNode.put(n.nodeId(), host);
                }
                networksById.remove(other.networkId);
            }
        }
        host.members.add(node);
        networkOfNode.put(node.nodeId(), host);
    }

    /** Removes a node and splits the host network if it becomes disconnected. */
    public void removeNode(@NotNull BlockKey key) {
        SimpleItemNode removed = nodesByKey.remove(key);
        if (removed == null) return;
        Network host = networkOfNode.remove(removed.nodeId());
        if (host == null) return;
        host.members.remove(removed);
        if (host.members.isEmpty()) {
            networksById.remove(host.networkId);
            return;
        }
        Set<UUID> seen = new HashSet<>();
        List<List<SimpleItemNode>> components = new ArrayList<>();
        for (SimpleItemNode start : host.members) {
            if (!seen.add(start.nodeId())) continue;
            List<SimpleItemNode> comp = new ArrayList<>();
            Deque<SimpleItemNode> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                SimpleItemNode cur = queue.removeFirst();
                comp.add(cur);
                for (SimpleItemNode adj : neighbours(cur.location())) {
                    if (seen.add(adj.nodeId())) {
                        queue.add(adj);
                    }
                }
            }
            components.add(comp);
        }
        if (components.size() == 1) {
            return; // still connected
        }
        ItemRoutingPolicy preserved = host.routingPolicy;
        networksById.remove(host.networkId);
        for (List<SimpleItemNode> comp : components) {
            Network n = new Network();
            n.routingPolicy = preserved;
            n.members.addAll(comp);
            networksById.put(n.networkId, n);
            for (SimpleItemNode m : comp) {
                networkOfNode.put(m.nodeId(), n);
            }
        }
    }

    public @NotNull Collection<ItemNetwork> networks() {
        return Collections.unmodifiableCollection(networksById.values());
    }

    public @NotNull Collection<SimpleItemNode> nodes() {
        return Collections.unmodifiableCollection(nodesByKey.values());
    }

    public SimpleItemNode nodeAt(@NotNull BlockKey key) {
        return nodesByKey.get(key);
    }

    public ItemNetwork networkOf(@NotNull ItemNode node) {
        return networkOfNode.get(node.nodeId());
    }

    public int networkCount() {
        return networksById.size();
    }

    public int nodeCount() {
        return nodesByKey.size();
    }

    /** Updates the routing policy of a network in place. */
    public void setRoutingPolicy(@NotNull UUID networkId, @NotNull ItemRoutingPolicy policy) {
        Network n = networksById.get(networkId);
        if (n != null) {
            n.routingPolicy = policy;
        }
    }

    public @NotNull Collection<SimpleItemNode> membersOf(@NotNull ItemNetwork network) {
        Network n = networksById.get(network.networkId());
        return n == null ? Collections.emptyList() : Collections.unmodifiableSet(n.members);
    }

    private Set<Network> neighbouringNetworks(BlockKey key) {
        Set<Network> out = new HashSet<>();
        for (SimpleItemNode adj : neighbours(key)) {
            Network n = networkOfNode.get(adj.nodeId());
            if (n != null) out.add(n);
        }
        return out;
    }

    private Iterable<SimpleItemNode> neighbours(BlockKey key) {
        List<SimpleItemNode> out = new ArrayList<>(6);
        check(out, key.world(), key.x() + 1, key.y(), key.z());
        check(out, key.world(), key.x() - 1, key.y(), key.z());
        check(out, key.world(), key.x(), key.y() + 1, key.z());
        check(out, key.world(), key.x(), key.y() - 1, key.z());
        check(out, key.world(), key.x(), key.y(), key.z() + 1);
        check(out, key.world(), key.x(), key.y(), key.z() - 1);
        return out;
    }

    private void check(List<SimpleItemNode> sink, String world, int x, int y, int z) {
        SimpleItemNode n = nodesByKey.get(new BlockKey(world, x, y, z));
        if (n != null) sink.add(n);
    }

    /** Internal mutable network. Public surface goes through {@link ItemNetwork}. */
    private static final class Network implements ItemNetwork {
        private final UUID networkId = UUID.randomUUID();
        private final Set<SimpleItemNode> members = new HashSet<>();
        private ItemRoutingPolicy routingPolicy = ItemRoutingPolicy.ROUND_ROBIN;

        @Override
        public UUID networkId() {
            return networkId;
        }

        @Override
        public Collection<ItemNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members));
        }

        @Override
        public ItemRoutingPolicy routingPolicy() {
            return routingPolicy;
        }

        @Override
        public int size() {
            return members.size();
        }
    }
}
