package dev.brmz.sapientia.core.energy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import org.junit.jupiter.api.Test;

final class NetworkGraphTest {

    @Test
    void singleNodeFormsItsOwnNetwork() {
        NetworkGraph g = new NetworkGraph();
        g.addNode(node(0, 0, 0, EnergyNodeType.GENERATOR));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void adjacentNodesMerge() {
        NetworkGraph g = new NetworkGraph();
        g.addNode(node(0, 0, 0, EnergyNodeType.GENERATOR));
        g.addNode(node(1, 0, 0, EnergyNodeType.CABLE));
        g.addNode(node(2, 0, 0, EnergyNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(1);
        EnergyNetwork only = g.networks().iterator().next();
        assertThat(only.size()).isEqualTo(3);
    }

    @Test
    void diagonalNodesDoNotConnect() {
        NetworkGraph g = new NetworkGraph();
        g.addNode(node(0, 0, 0, EnergyNodeType.GENERATOR));
        g.addNode(node(1, 1, 0, EnergyNodeType.GENERATOR));
        assertThat(g.networkCount()).isEqualTo(2);
    }

    @Test
    void breakingCableInTheMiddleSplitsTheNetwork() {
        NetworkGraph g = new NetworkGraph();
        g.addNode(node(0, 0, 0, EnergyNodeType.GENERATOR));
        g.addNode(node(1, 0, 0, EnergyNodeType.CABLE));
        g.addNode(node(2, 0, 0, EnergyNodeType.CABLE));
        g.addNode(node(3, 0, 0, EnergyNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(1);

        g.removeNode(new BlockKey("w", 1, 0, 0));
        assertThat(g.networkCount()).isEqualTo(2);
        assertThat(g.nodeCount()).isEqualTo(3);
    }

    @Test
    void mergingTwoNetworksWithACable() {
        NetworkGraph g = new NetworkGraph();
        g.addNode(node(0, 0, 0, EnergyNodeType.GENERATOR));
        g.addNode(node(2, 0, 0, EnergyNodeType.CONSUMER));
        assertThat(g.networkCount()).isEqualTo(2);

        g.addNode(node(1, 0, 0, EnergyNodeType.CABLE));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void offerAndDrawRespectBufferLimits() {
        SimpleEnergyNode n = new SimpleEnergyNode(
                UUID.randomUUID(), new BlockKey("w", 0, 0, 0),
                EnergyNodeType.CAPACITOR, EnergyTier.LOW, 0L, 100L);
        assertThat(n.offer(60)).isEqualTo(60);
        assertThat(n.offer(60)).isEqualTo(40); // capped at bufferMax
        assertThat(n.bufferCurrent()).isEqualTo(100);
        assertThat(n.draw(150)).isEqualTo(100);
        assertThat(n.bufferCurrent()).isZero();
    }

    private static SimpleEnergyNode node(int x, int y, int z, EnergyNodeType type) {
        return new SimpleEnergyNode(
                UUID.randomUUID(), new BlockKey("w", x, y, z), type, EnergyTier.LOW, 0L, 1000L);
    }
}
