package dev.brmz.sapientia.core.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaEnergyFlowEvent;
import dev.brmz.sapientia.core.block.BlockKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class EnergySolverTest {

    private static final long CONSUMPTION = EnergySpecs.consumptionPerTick(EnergyTier.LOW);
    private static final long GENERATION = EnergySpecs.generationPerTick(EnergyTier.LOW);

    private final NetworkGraph graph = new NetworkGraph();
    private final List<SapientiaEnergyFlowEvent> events = new ArrayList<>();
    private final EnergySolver solver = new EnergySolver(graph, events::add);

    @Test
    void consumersAreToppedUpFromCapacitors() {
        SimpleEnergyNode cap = node(0, EnergyNodeType.CAPACITOR, 1_000, 1_000);
        SimpleEnergyNode cons = node(1, EnergyNodeType.CONSUMER, 0, 100);
        graph.addNode(cap);
        graph.addNode(cons);

        solver.tick();

        assertThat(cons.bufferCurrent()).isEqualTo(CONSUMPTION);
        assertThat(cap.bufferCurrent()).isEqualTo(1_000 - CONSUMPTION);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).consumed()).isEqualTo(CONSUMPTION);
    }

    @Test
    void generatorsFillTheirBufferAndSpillIntoCapacitors() {
        SimpleEnergyNode gen = node(0, EnergyNodeType.GENERATOR, 0, 10);
        SimpleEnergyNode cap = node(1, EnergyNodeType.CAPACITOR, 0, 1_000);
        graph.addNode(gen);
        graph.addNode(cap);

        solver.tick();

        assertThat(gen.bufferCurrent()).isEqualTo(10);
        assertThat(cap.bufferCurrent()).isEqualTo(GENERATION - 10);
        assertThat(events.get(0).generated()).isEqualTo(GENERATION);
    }

    @Test
    void aSettledNetworkSleepsUntilAMachineDrawsEnergy() {
        SimpleEnergyNode cap = node(0, EnergyNodeType.CAPACITOR, 1_000, 1_000);
        SimpleEnergyNode cons = node(1, EnergyNodeType.CONSUMER, 0, CONSUMPTION);
        graph.addNode(cap);
        graph.addNode(cons);
        solver.tick(); // fills the consumer
        solver.tick(); // nothing to do: goes to sleep
        events.clear();

        for (int i = 0; i < 50; i++) solver.tick();
        assertThat(events).isEmpty();
        assertThat(cap.bufferCurrent()).isEqualTo(1_000 - CONSUMPTION);

        cons.draw(CONSUMPTION); // a machine spends its energy
        solver.tick();

        assertThat(cons.bufferCurrent()).isEqualTo(CONSUMPTION);
        assertThat(events).hasSize(1);
    }

    @Test
    void nodesInInactiveChunksAreSkipped() {
        SimpleEnergyNode cap = node(0, EnergyNodeType.CAPACITOR, 1_000, 1_000);
        SimpleEnergyNode cons = node(1, EnergyNodeType.CONSUMER, 0, 100);
        graph.addNode(cap);
        graph.addNode(cons);
        graph.onChunkDeactivated("w", 0, 0);

        solver.tick();
        assertThat(cons.bufferCurrent()).isZero();

        graph.onChunkActivated("w", 0, 0);
        solver.tick();
        assertThat(cons.bufferCurrent()).isEqualTo(CONSUMPTION);
    }

    @Test
    void onAnotherThreadEventsArriveOnTheNextTick() {
        solver.runOn(Runnable::run, Logger.getLogger("EnergySolverTest"));
        graph.addNode(node(0, EnergyNodeType.CAPACITOR, 1_000, 1_000));
        graph.addNode(node(1, EnergyNodeType.CONSUMER, 0, 100));

        solver.tick();
        assertThat(events).isEmpty();

        solver.tick();
        assertThat(events).isNotEmpty();
    }

    @Test
    void cablesAreNeverVisited() {
        graph.addNode(node(0, EnergyNodeType.CAPACITOR, 1_000, 1_000));
        for (int x = 1; x < 100; x++) graph.addNode(node(x, EnergyNodeType.CABLE, 0, 10));
        graph.addNode(node(100, EnergyNodeType.CONSUMER, 0, 100));

        solver.tick();

        assertThat(graph.nodes()).filteredOn(n -> n.type() == EnergyNodeType.CABLE)
                .allMatch(n -> n.bufferCurrent() == 0);
        assertThat(events.get(0).consumed()).isEqualTo(CONSUMPTION);
    }

    private static SimpleEnergyNode node(int x, EnergyNodeType type, long current, long max) {
        return new SimpleEnergyNode(UUID.randomUUID(), new BlockKey("w", x, 64, 0), type, EnergyTier.LOW, current, max);
    }
}
