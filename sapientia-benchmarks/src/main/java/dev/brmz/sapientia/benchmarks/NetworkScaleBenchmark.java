package dev.brmz.sapientia.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.energy.EnergySolver;
import dev.brmz.sapientia.core.energy.NetworkGraph;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Foundation 1 scale benchmark for networks: energy nodes spread over the
 * world as small networks (one capacitor, eight cables, one consumer, all in
 * one chunk), up to 10 million nodes.
 *
 * <ul>
 *   <li>{@code solverCycle}: one energy cycle while 1% of the consumers are
 *       drawn on by machines. Settled networks sleep, so the cost follows the
 *       networks that changed, not the nodes on the server.</li>
 *   <li>{@code chunkUnloadAndLoad}: one chunk leaves the graph and comes
 *       back; costs the nodes of that chunk only.</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
@Fork(value = 1, jvmArgsAppend = "-Xmx8g")
@State(Scope.Benchmark)
public class NetworkScaleBenchmark {

    private static final int NODES_PER_NETWORK = 10;
    private static final String WORLD = "bench";

    @Param({"100000", "1000000", "10000000"})
    public int nodes;

    private NetworkGraph graph;
    private EnergySolver solver;
    private List<SimpleEnergyNode> consumers;
    private int networks;
    private int drawCursor;
    private int chunkCursor;

    @Setup(Level.Trial)
    public void setUp() {
        graph = new NetworkGraph();
        solver = new EnergySolver(graph, event -> { });
        networks = nodes / NODES_PER_NETWORK;
        consumers = new ArrayList<>(networks);
        int side = (int) Math.ceil(Math.sqrt(networks));
        for (int n = 0; n < networks; n++) {
            // One network per chunk, laid out on a square of chunks.
            int baseX = (n % side) * 16;
            int baseZ = (n / side) * 16;
            graph.addNode(node(baseX, baseZ, EnergyNodeType.CAPACITOR, 1_000_000, 1_000_000));
            for (int c = 1; c <= 8; c++) {
                graph.addNode(node(baseX + c, baseZ, EnergyNodeType.CABLE, 0, 1));
            }
            SimpleEnergyNode consumer = node(baseX + 9, baseZ, EnergyNodeType.CONSUMER, 0, 16);
            graph.addNode(consumer);
            consumers.add(consumer);
        }
        // Let every network settle: consumers full, then asleep.
        for (int i = 0; i < 4; i++) {
            solver.tick();
        }
    }

    @Benchmark
    public void solverCycle(Blackhole blackhole) {
        int draws = Math.max(1, networks / 100);
        for (int i = 0; i < draws; i++) {
            consumers.get(drawCursor).draw(16);
            drawCursor = (drawCursor + 1) % networks;
        }
        solver.tick();
        blackhole.consume(drawCursor);
    }

    @Benchmark
    public void chunkUnloadAndLoad(Blackhole blackhole) {
        int side = (int) Math.ceil(Math.sqrt(networks));
        int n = chunkCursor;
        chunkCursor = (chunkCursor + 1) % networks;
        List<SimpleEnergyNode> removed = graph.removeChunk(WORLD, n % side, n / side);
        for (SimpleEnergyNode node : removed) {
            graph.addNode(node);
        }
        blackhole.consume(removed.size());
    }

    private static SimpleEnergyNode node(int x, int z, EnergyNodeType type, long current, long max) {
        return new SimpleEnergyNode(UUID.randomUUID(), new BlockKey(WORLD, x, 64, z), type, EnergyTier.LOW, current, max);
    }
}
