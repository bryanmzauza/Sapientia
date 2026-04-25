package dev.brmz.sapientia.core.fluids;

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

import dev.brmz.sapientia.api.fluids.FluidNetwork;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * In-memory adjacency graph of {@link SimpleFluidNode}s grouped into connected
 * {@link FluidNetwork}s (T-301 / 1.2.0). Direct port of {@code ItemNetworkGraph}
 * — same 6-neighbour BFS, split-on-removal, merge-on-add. See ADR-013/015.
 */
public final class FluidNetworkGraph {

    private final Map<BlockKey, SimpleFluidNode> nodesByKey = new HashMap<>();
    private final Map<UUID, Network> networksById = new LinkedHashMap<>();
    private final Map<UUID, Network> networkOfNode = new HashMap<>();

    public void addNode(@NotNull SimpleFluidNode node) {
        BlockKey key = node.location();
        if (nodesByKey.putIfAbsent(key, node) != null) return;
        Set<Network> neighbours = neighbouringNetworks(key);
        Network host;
        if (neighbours.isEmpty()) {
            host = new Network();
            networksById.put(host.networkId, host);
        } else {
            host = neighbours.stream().max((a, b) -> Integer.compare(a.size(), b.size())).orElseThrow();
            for (Network other : neighbours) {
                if (other == host) continue;
                for (SimpleFluidNode n : other.members) {
                    host.members.add(n);
                    networkOfNode.put(n.nodeId(), host);
                }
                networksById.remove(other.networkId);
            }
        }
        host.members.add(node);
        networkOfNode.put(node.nodeId(), host);
    }

    public void removeNode(@NotNull BlockKey key) {
        SimpleFluidNode removed = nodesByKey.remove(key);
        if (removed == null) return;
        Network host = networkOfNode.remove(removed.nodeId());
        if (host == null) return;
        host.members.remove(removed);
        if (host.members.isEmpty()) {
            networksById.remove(host.networkId);
            return;
        }
        Set<UUID> seen = new HashSet<>();
        List<List<SimpleFluidNode>> components = new ArrayList<>();
        for (SimpleFluidNode start : host.members) {
            if (!seen.add(start.nodeId())) continue;
            List<SimpleFluidNode> comp = new ArrayList<>();
            Deque<SimpleFluidNode> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                SimpleFluidNode cur = queue.removeFirst();
                comp.add(cur);
                for (SimpleFluidNode adj : neighbours(cur.location())) {
                    if (seen.add(adj.nodeId())) queue.add(adj);
                }
            }
            components.add(comp);
        }
        if (components.size() == 1) return;
        networksById.remove(host.networkId);
        for (List<SimpleFluidNode> comp : components) {
            Network n = new Network();
            n.members.addAll(comp);
            networksById.put(n.networkId, n);
            for (SimpleFluidNode m : comp) networkOfNode.put(m.nodeId(), n);
        }
    }

    public @NotNull Collection<FluidNetwork> networks() {
        return Collections.unmodifiableCollection(networksById.values());
    }

    public @NotNull Collection<SimpleFluidNode> nodes() {
        return Collections.unmodifiableCollection(nodesByKey.values());
    }

    public SimpleFluidNode nodeAt(@NotNull BlockKey key) {
        return nodesByKey.get(key);
    }

    public FluidNetwork networkOf(@NotNull FluidNode node) {
        return networkOfNode.get(node.nodeId());
    }

    public int networkCount() { return networksById.size(); }
    public int nodeCount() { return nodesByKey.size(); }

    public @NotNull Collection<SimpleFluidNode> membersOf(@NotNull FluidNetwork network) {
        Network n = networksById.get(network.networkId());
        return n == null ? Collections.emptyList() : Collections.unmodifiableSet(n.members);
    }

    private Set<Network> neighbouringNetworks(BlockKey key) {
        Set<Network> out = new HashSet<>();
        for (SimpleFluidNode adj : neighbours(key)) {
            Network n = networkOfNode.get(adj.nodeId());
            if (n != null) out.add(n);
        }
        return out;
    }

    private Iterable<SimpleFluidNode> neighbours(BlockKey key) {
        List<SimpleFluidNode> out = new ArrayList<>(6);
        check(out, key.world(), key.x() + 1, key.y(), key.z());
        check(out, key.world(), key.x() - 1, key.y(), key.z());
        check(out, key.world(), key.x(), key.y() + 1, key.z());
        check(out, key.world(), key.x(), key.y() - 1, key.z());
        check(out, key.world(), key.x(), key.y(), key.z() + 1);
        check(out, key.world(), key.x(), key.y(), key.z() - 1);
        return out;
    }

    private void check(List<SimpleFluidNode> sink, String world, int x, int y, int z) {
        SimpleFluidNode n = nodesByKey.get(new BlockKey(world, x, y, z));
        if (n != null) sink.add(n);
    }

    private static final class Network implements FluidNetwork {
        private final UUID networkId = UUID.randomUUID();
        private final Set<SimpleFluidNode> members = new HashSet<>();

        @Override public UUID networkId() { return networkId; }
        @Override public Collection<FluidNode> nodes() {
            return Collections.unmodifiableCollection(new ArrayList<>(members));
        }
        @Override public int size() { return members.size(); }
    }
}
