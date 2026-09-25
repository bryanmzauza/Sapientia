package dev.brmz.sapientia.core.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.engine.ActivityFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Shared network graph behaviour (chunk index, splits, roles, pacing), exercised through the energy graph. */
final class NodeGraphTest {

    private final NetworkGraph graph = new NetworkGraph();

    @Test
    void unloadingAChunkRemovesOnlyItsNodesAndSplitsTheLine() {
        for (int x = 0; x < 48; x++) {
            graph.addNode(node(x, 64, 0, x == 20 ? EnergyNodeType.CAPACITOR : EnergyNodeType.CABLE));
        }
        assertThat(graph.networkCount()).isEqualTo(1);

        List<SimpleEnergyNode> removed = graph.removeChunk("w", 1, 0);

        // Only working nodes come back (they may hold unsaved state); cables are primitive entries.
        assertThat(removed).singleElement().satisfies(n -> assertThat(n.location().x()).isEqualTo(20));
        assertThat(graph.nodeCount()).isEqualTo(32);
        assertThat(graph.networkCount()).isEqualTo(2);
        assertThat(graph.nodesInChunk("w", 1, 0)).isEmpty();
        assertThat(graph.nodesInChunk("w", 0, 0)).hasSize(16);
        assertThat(graph.nodeAt(new BlockKey("w", 20, 64, 0))).isNull();
    }

    @Test
    void cablesComeBackAsViewsWithAStableId() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.CABLE));
        graph.addNode(node(1, 64, 0, EnergyNodeType.CONSUMER));

        SimpleEnergyNode first = graph.nodeAt(new BlockKey("w", 0, 64, 0));
        SimpleEnergyNode second = graph.nodeAt(new BlockKey("w", 0, 64, 0));

        assertThat(first).isNotNull().isNotSameAs(second);
        assertThat(first.type()).isEqualTo(EnergyNodeType.CABLE);
        assertThat(first.nodeId()).isEqualTo(second.nodeId()).isEqualTo(NetworkGraph.transitId(first.location()));
        assertThat(graph.networkOf(first)).isSameAs(graph.networkOf(graph.nodeAt(new BlockKey("w", 1, 64, 0))));
        assertThat(only().members()).hasSize(2);
    }

    @Test
    void negativeCoordinatesLandInTheRightChunk() {
        graph.addNode(node(-1, 64, -1, EnergyNodeType.CABLE));
        graph.addNode(node(-16, 64, -16, EnergyNodeType.CABLE));
        graph.addNode(node(-17, 64, -1, EnergyNodeType.CABLE));

        assertThat(graph.nodesInChunk("w", -1, -1)).hasSize(2);
        assertThat(graph.nodesInChunk("w", -2, -1)).hasSize(1);
    }

    @Test
    void removingAPieceOfALoopKeepsOneNetwork() {
        for (int x = 0; x < 5; x++) {
            graph.addNode(node(x, 64, 0, EnergyNodeType.CABLE));
            graph.addNode(node(x, 64, 4, EnergyNodeType.CABLE));
        }
        for (int z = 1; z < 4; z++) {
            graph.addNode(node(0, 64, z, EnergyNodeType.CABLE));
            graph.addNode(node(4, 64, z, EnergyNodeType.CABLE));
        }
        NetworkGraph.Network before = only();

        graph.removeNode(new BlockKey("w", 2, 64, 0));

        assertThat(graph.networkCount()).isEqualTo(1);
        assertThat(only()).isSameAs(before);
        assertThat(only().size()).isEqualTo(15);
    }

    @Test
    void removingTheHubOfACrossLeavesFourNetworks() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.CABLE));
        for (int d = 1; d <= 3; d++) {
            graph.addNode(node(d, 64, 0, EnergyNodeType.CABLE));
            graph.addNode(node(-d, 64, 0, EnergyNodeType.CABLE));
            graph.addNode(node(0, 64, d, EnergyNodeType.CABLE));
            graph.addNode(node(0, 64, -d, EnergyNodeType.CABLE));
        }

        graph.removeNode(new BlockKey("w", 0, 64, 0));

        assertThat(graph.networkCount()).isEqualTo(4);
        assertThat(graph.groups()).allMatch(n -> n.size() == 3);
    }

    @Test
    void mergingMovesMembersAndRetiresTheSmallerNetwork() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.GENERATOR));
        NetworkGraph.Network small = only();
        graph.addNode(node(2, 64, 0, EnergyNodeType.CONSUMER));
        graph.addNode(node(3, 64, 0, EnergyNodeType.CABLE));

        graph.addNode(node(1, 64, 0, EnergyNodeType.CABLE));

        assertThat(graph.networkCount()).isEqualTo(1);
        assertThat(small.isRemoved()).isTrue();
        NetworkGraph.Network merged = only();
        assertThat(merged.size()).isEqualTo(4);
        assertThat(merged.role(NetworkGraph.GENERATORS)).hasSize(1);
        assertThat(merged.role(NetworkGraph.CONSUMERS)).hasSize(1);
        assertThat(merged.role(NetworkGraph.CAPACITORS)).isEmpty();
    }

    @Test
    void rolesFollowSplits() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.GENERATOR));
        graph.addNode(node(1, 64, 0, EnergyNodeType.CABLE));
        graph.addNode(node(2, 64, 0, EnergyNodeType.CONSUMER));

        graph.removeNode(new BlockKey("w", 1, 64, 0));

        long withGenerator = graph.groups().stream().filter(n -> n.role(NetworkGraph.GENERATORS).size() == 1).count();
        long withConsumer = graph.groups().stream().filter(n -> n.role(NetworkGraph.CONSUMERS).size() == 1).count();
        assertThat(withGenerator).isEqualTo(1);
        assertThat(withConsumer).isEqualTo(1);
    }

    @Test
    void networksRunWhenWokenAndBackOffWhenIdle() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.CABLE));
        NetworkGraph.Network net = only();

        assertThat(graph.collectDue(1)).containsExactly(net); // new networks start awake
        graph.idle(net, 1, 32);
        assertThat(graph.collectDue(2)).containsExactly(net); // back-off 1
        graph.idle(net, 2, 32);
        assertThat(graph.collectDue(3)).isEmpty();
        assertThat(graph.collectDue(4)).containsExactly(net); // back-off 2
        graph.idle(net, 4, 32);
        for (long c = 5; c < 8; c++) assertThat(graph.collectDue(c)).isEmpty();
        assertThat(graph.collectDue(8)).containsExactly(net); // back-off 4

        graph.idle(net, 8, 0); // sleep
        for (long c = 9; c < 200; c++) assertThat(graph.collectDue(c)).isEmpty();
        net.wake();
        assertThat(graph.collectDue(200)).containsExactly(net);
        graph.worked(net, 200);
        assertThat(graph.collectDue(201)).containsExactly(net);
    }

    @Test
    void wakingTwiceRunsOnce() {
        graph.addNode(node(0, 64, 0, EnergyNodeType.CABLE));
        NetworkGraph.Network net = only();
        graph.collectDue(1);
        graph.worked(net, 1);
        net.wake();
        net.wake();

        assertThat(graph.collectDue(2)).containsExactly(net);
    }

    @Test
    void onlyChangedNodesArePersistedAndRemovedOnesAreSkipped() {
        SimpleEnergyNode a = node(0, 64, 0, EnergyNodeType.CAPACITOR);
        SimpleEnergyNode b = node(5, 64, 0, EnergyNodeType.CAPACITOR);
        SimpleEnergyNode c = node(9, 64, 0, EnergyNodeType.CAPACITOR);
        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        a.offer(10);
        a.offer(10);
        b.offer(10);
        graph.removeNode(b.location());

        List<SimpleEnergyNode> saved = new ArrayList<>();
        graph.drainDirty(saved::add);

        assertThat(saved).containsExactly(a);
        saved.clear();
        graph.drainDirty(saved::add);
        assertThat(saved).isEmpty();
    }

    @Test
    void externalChangesWakeASleepingNetwork() {
        SimpleEnergyNode cap = node(0, 64, 0, EnergyNodeType.CAPACITOR);
        graph.addNode(cap);
        NetworkGraph.Network net = only();
        graph.collectDue(1);
        graph.idle(net, 1, 0);

        cap.offer(5);

        assertThat(graph.collectDue(2)).containsExactly(net);
    }

    @Test
    void chunkActivationTogglesNodesAndWakesTheirNetwork() {
        graph.setActivityFilter(new ActivityFilter() {
            @Override
            public boolean isActive(@NotNull String world, int chunkX, int chunkZ) {
                return false;
            }
        });
        SimpleEnergyNode n = node(0, 64, 0, EnergyNodeType.CONSUMER);
        graph.addNode(n);
        NetworkGraph.Network net = only();
        graph.collectDue(1);
        graph.idle(net, 1, 0);
        assertThat(n.isActive()).isFalse();

        graph.onChunkActivated("w", 0, 0);

        assertThat(n.isActive()).isTrue();
        assertThat(graph.collectDue(2)).containsExactly(net);
        graph.onChunkDeactivated("w", 0, 0);
        assertThat(n.isActive()).isFalse();
    }

    @Test
    void cuttingALongLineStaysFast() {
        int length = 200_000;
        for (int x = 0; x < length; x++) graph.addNode(node(x, 64, 0, EnergyNodeType.CABLE));

        long start = System.nanoTime();
        graph.removeNode(new BlockKey("w", length - 3, 64, 0)); // cuts off a two-node stub
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(graph.networkCount()).isEqualTo(2);
        assertThat(graph.nodeCount()).isEqualTo(length - 1);
        assertThat(elapsedMs).isLessThan(2_000);
    }

    private NetworkGraph.Network only() {
        assertThat(graph.networkCount()).isEqualTo(1);
        return graph.groups().get(0);
    }

    private static SimpleEnergyNode node(int x, int y, int z, EnergyNodeType type) {
        return new SimpleEnergyNode(UUID.randomUUID(), new BlockKey("w", x, y, z), type, EnergyTier.LOW, 0L, 1000L);
    }
}
