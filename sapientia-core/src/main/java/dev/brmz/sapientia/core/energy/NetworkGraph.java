package dev.brmz.sapientia.core.energy;

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

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory adjacency graph of {@link SimpleEnergyNode}s grouped into connected
 * components ({@link EnergyNetwork}s). Nodes are adjacent when their {@link BlockKey}s
 * differ by 1 along exactly one axis (6-neighborhood) and are in the same world.
 *
 * <p>Mutations ({@link #addNode}, {@link #removeNode}) recompute the affected
 * networks lazily by BFS. {@link #networks()} returns a stable read-only view for
 * the solver. Designed to be called from the main/region thread; not thread-safe.
 *
 * <p>See ROADMAP 0.3.0 (T-141).
 */
public final class NetworkGraph {

    private final Map<BlockKey, SimpleEnergyNode> nodesByKey = new HashMap<>();
    private final Map<UUID, Network> networksById = new LinkedHashMap<>();
    private final Map<UUID, Network> networkOfNode = new HashMap<>();

    /** Adds a node to the graph, merging or extending networks as needed. */
    public void addNode(@NotNull SimpleEnergyNode node) {
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
            // Merge into the largest neighbour to minimise re-bucketing cost.
            host = neighbours.stream().max((a, b) -> Integer.compare(a.size(), b.size())).orElseThrow();
            for (Network other : neighbours) {
                if (other == host) continue;
                for (SimpleEnergyNode n : other.members) {
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
        SimpleEnergyNode removed = nodesByKey.remove(key);
        if (removed == null) return;
        Network host = networkOfNode.remove(removed.nodeId());
        if (host == null) return;
        host.members.remove(removed);
        if (host.members.isEmpty()) {
            networksById.remove(host.networkId);
            return;
        }
        // Re-flood from each remaining member; collect components.
        Set<UUID> seen = new HashSet<>();
        List<List<SimpleEnergyNode>> components = new ArrayList<>();
        for (SimpleEnergyNode start : host.members) {
            if (!seen.add(start.nodeId())) continue;
            List<SimpleEnergyNode> comp = new ArrayList<>();
            Deque<SimpleEnergyNode> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                SimpleEnergyNode cur = queue.removeFirst();
                comp.add(cur);
                for (SimpleEnergyNode adj : neighbours(cur.location())) {
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
        networksById.remove(host.networkId);
        for (List<SimpleEnergyNode> comp : components) {
            Network n = new Network();
            n.members.addAll(comp);
            networksById.put(n.networkId, n);
            for (SimpleEnergyNode m : comp) {
                networkOfNode.put(m.nodeId(), n);
            }
        }
    }

    public @NotNull Collection<EnergyNetwork> networks() {
        return Collections.unmodifiableCollection(networksById.values());
    }

    public @NotNull Collection<SimpleEnergyNode> nodes() {
        return Collections.unmodifiableCollection(nodesByKey.values());
    }

    public SimpleEnergyNode nodeAt(@NotNull BlockKey key) {
        return nodesByKey.get(key);
    }

    public EnergyNetwork networkOf(@NotNull EnergyNode node) {
        return networkOfNode.get(node.nodeId());
    }

    public int networkCount() {
        return networksById.size();
    }

    public int nodeCount() {
        return nodesByKey.size();
    }

    private Set<Network> neighbouringNetworks(BlockKey key) {
        Set<Network> out = new HashSet<>();
        for (SimpleEnergyNode adj : neighbours(key)) {
            Network n = networkOfNode.get(adj.nodeId());
            if (n != null) out.add(n);
        }
        return out;
    }

    private Iterable<SimpleEnergyNode> neighbours(BlockKey key) {
        List<SimpleEnergyNode> out = new ArrayList<>(6);
        check(out, key.world(), key.x() + 1, key.y(), key.z());
        check(out, key.world(), key.x() - 1, key.y(), key.z());
        check(out, key.world(), key.x(), key.y() + 1, key.z());
        check(out, key.world(), key.x(), key.y() - 1, key.z());
        check(out, key.world(), key.x(), key.y(), key.z() + 1);
        check(out, key.world(), key.x(), key.y(), key.z() - 1);
        return out;
    }

    private void check(List<SimpleEnergyNode> sink, String world, int x, int y, int z) {
        SimpleEnergyNode n = nodesByKey.get(new BlockKey(world, x, y, z));
        if (n != null) sink.add(n);
    }

    /** Internal mutable network. Public surface goes through {@link EnergyNetwork}. */
    private static final class Network implements EnergyNetwork {
        private final UUID networkId = UUID.randomUUID();
        private final Set<SimpleEnergyNode> members = new HashSet<>();

        @Override
        public UUID networkId() {
            return networkId;
        }

        @Override
        public Collection<EnergyNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members));
        }

        @Override
        public long totalStored() {
            long t = 0;
            for (SimpleEnergyNode m : members) t += m.bufferCurrent();
            return t;
        }

        @Override
        public long totalCapacity() {
            long t = 0;
            for (SimpleEnergyNode m : members) t += m.bufferMax();
            return t;
        }

        @Override
        public int size() {
            return members.size();
        }
    }

    /** Test/diagnostic accessor returning the mutable members of a network. */
    public Collection<SimpleEnergyNode> membersOf(@NotNull EnergyNetwork network) {
        Network n = networksById.get(network.networkId());
        return n == null ? Collections.emptyList() : Collections.unmodifiableSet(n.members);
    }
}
