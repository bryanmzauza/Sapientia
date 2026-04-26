package dev.brmz.sapientia.core.fluids;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNetwork;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

/**
 * Mirrors {@code ItemNetworkGraphTest} for the fluid logistics graph
 * (T-301 / 1.2.0). Same 6-neighbour BFS contract over {@link FluidNetworkGraph}
 * + {@link SimpleFluidNode}, plus tank capacity / no-mixing assertions.
 */
final class FluidNetworkGraphTest {

    private static final FluidType WATER = new FluidType(
            new NamespacedKey("sapientia", "water"), "fluid.water.name", 0x3F76E4, 1000, false);
    private static final FluidType LAVA = new FluidType(
            new NamespacedKey("sapientia", "lava"), "fluid.lava.name", 0xFF6A00, 3000, true);

    @Test
    void singleNodeFormsItsOwnNetwork() {
        FluidNetworkGraph g = new FluidNetworkGraph();
        g.addNode(node(0, 0, 0, FluidNodeType.PUMP));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void adjacentNodesMerge() {
        FluidNetworkGraph g = new FluidNetworkGraph();
        g.addNode(node(0, 0, 0, FluidNodeType.PUMP));
        g.addNode(node(1, 0, 0, FluidNodeType.PIPE));
        g.addNode(node(2, 0, 0, FluidNodeType.TANK));
        assertThat(g.networkCount()).isEqualTo(1);
        FluidNetwork only = g.networks().iterator().next();
        assertThat(only.size()).isEqualTo(3);
    }

    @Test
    void diagonalNodesDoNotConnect() {
        FluidNetworkGraph g = new FluidNetworkGraph();
        g.addNode(node(0, 0, 0, FluidNodeType.PUMP));
        g.addNode(node(1, 1, 0, FluidNodeType.PUMP));
        assertThat(g.networkCount()).isEqualTo(2);
    }

    @Test
    void breakingPipeInTheMiddleSplitsTheNetwork() {
        FluidNetworkGraph g = new FluidNetworkGraph();
        g.addNode(node(0, 0, 0, FluidNodeType.PUMP));
        g.addNode(node(1, 0, 0, FluidNodeType.PIPE));
        g.addNode(node(2, 0, 0, FluidNodeType.PIPE));
        g.addNode(node(3, 0, 0, FluidNodeType.DRAIN));
        assertThat(g.networkCount()).isEqualTo(1);

        g.removeNode(new BlockKey("w", 1, 0, 0));
        assertThat(g.networkCount()).isEqualTo(2);
        assertThat(g.nodeCount()).isEqualTo(3);
    }

    @Test
    void mergingTwoNetworksWithAPipe() {
        FluidNetworkGraph g = new FluidNetworkGraph();
        g.addNode(node(0, 0, 0, FluidNodeType.PUMP));
        g.addNode(node(2, 0, 0, FluidNodeType.DRAIN));
        assertThat(g.networkCount()).isEqualTo(2);

        g.addNode(node(1, 0, 0, FluidNodeType.PIPE));
        assertThat(g.networkCount()).isEqualTo(1);
    }

    @Test
    void tankOfferRespectsCapacityAndRejectsMixing() {
        SimpleFluidNode tank = new SimpleFluidNode(
                UUID.randomUUID(), new BlockKey("w", 0, 0, 0),
                FluidNodeType.TANK, EnergyTier.LOW, null, 0L);
        long capacity = tank.capacityMb();
        assertThat(capacity).isPositive();

        // Empty tank adopts water.
        long firstAccepted = tank.offer(WATER, capacity + 500);
        assertThat(firstAccepted).isEqualTo(capacity);

        FluidStack contents = tank.contents();
        assertThat(contents).isNotNull();
        assertThat(contents.type().id()).isEqualTo(WATER.id());
        assertThat(contents.amountMb()).isEqualTo(capacity);

        // Already full \u2014 nothing accepted.
        assertThat(tank.offer(WATER, 1000)).isZero();

        // Drain so something can be reinserted.
        long drawn = tank.draw(500);
        assertThat(drawn).isEqualTo(500);

        // No mixing \u2014 lava is rejected while water is still inside.
        assertThat(tank.offer(LAVA, 500)).isZero();
        assertThat(tank.contents().type().id()).isEqualTo(WATER.id());

        // Drain to empty \u2014 then lava can be accepted.
        tank.draw(Long.MAX_VALUE);
        assertThat(tank.contents()).isNull();
        assertThat(tank.offer(LAVA, 500)).isEqualTo(500);
        assertThat(tank.contents().type().id()).isEqualTo(LAVA.id());
    }

    private static SimpleFluidNode node(int x, int y, int z, FluidNodeType type) {
        return new SimpleFluidNode(
                UUID.randomUUID(), new BlockKey("w", x, y, z), type, EnergyTier.LOW, null, 0L);
    }
}
