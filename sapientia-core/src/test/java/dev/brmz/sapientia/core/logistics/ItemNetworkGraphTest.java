package dev.brmz.sapientia.core.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.core.block.BlockKey;
import org.junit.jupiter.api.Test;

/**
 * Mirrors {@code NetworkGraphTest} for the item logistics graph (T-300 / 1.1.0).
 * Same 6-neighbour BFS contract — just over {@link ItemNetworkGraph} +
 * {@link SimpleItemNode}.
 */
final class ItemNetworkGraphTest {

    @Test
    void singleNodeFormsItsOwnNetwork() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void adjacentNodesMerge() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        g.addNode(node(1, 0, 0, ItemNodeType.CABLE));
        g.addNode(node(2, 0, 0, ItemNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(1);
        ItemNetwork only = g.networks().iterator().next();
        assertThat(only.size()).isEqualTo(3);
    }

    @Test
    void diagonalNodesDoNotConnect() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        g.addNode(node(1, 1, 0, ItemNodeType.PRODUCER));
        assertThat(g.networkCount()).isEqualTo(2);
    }

    @Test
    void breakingCableInTheMiddleSplitsTheNetwork() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        g.addNode(node(1, 0, 0, ItemNodeType.CABLE));
        g.addNode(node(2, 0, 0, ItemNodeType.CABLE));
        g.addNode(node(3, 0, 0, ItemNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(1);

        g.removeNode(new BlockKey("w", 1, 0, 0));
        assertThat(g.networkCount()).isEqualTo(2);
        assertThat(g.nodeCount()).isEqualTo(3);
    }

    @Test
    void mergingTwoNetworksWithACable() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        g.addNode(node(2, 0, 0, ItemNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(2);

        g.addNode(node(1, 0, 0, ItemNodeType.CABLE));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void routingPolicyDefaultsToRoundRobinAndCanBeChanged() {
        ItemNetworkGraph g = new ItemNetworkGraph();
        g.addNode(node(0, 0, 0, ItemNodeType.PRODUCER));
        ItemNetwork net = g.networks().iterator().next();
        assertThat(net.routingPolicy()).isEqualTo(ItemRoutingPolicy.ROUND_ROBIN);

        g.setRoutingPolicy(net.networkId(), ItemRoutingPolicy.PRIORITY);
        assertThat(net.routingPolicy()).isEqualTo(ItemRoutingPolicy.PRIORITY);
    }

    private static SimpleItemNode node(int x, int y, int z, ItemNodeType type) {
        return new SimpleItemNode(
                UUID.randomUUID(), new BlockKey("w", x, y, z), type, EnergyTier.LOW, 0);
    }
}
