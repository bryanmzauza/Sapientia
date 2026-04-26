package dev.brmz.sapientia.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.energy.NetworkGraph;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * P-003 — Energy graph: 500 cable-connected nodes, full rebuild. Measures the
 * cost of assembling the adjacency graph via {@link NetworkGraph#addNode},
 * exercising the merge-on-add path that dominates real-world reloads.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class NetworkGraphBenchmark {

    private static final int NODE_COUNT = 500;
    private static final String WORLD = "bench";

    private List<SimpleEnergyNode> nodes;

    @Setup
    public void setUp() {
        nodes = new ArrayList<>(NODE_COUNT);
        // Straight cable line → one connected network of 500 nodes.
        for (int i = 0; i < NODE_COUNT; i++) {
            nodes.add(new SimpleEnergyNode(
                    UUID.randomUUID(),
                    new BlockKey(WORLD, i, 64, 0),
                    EnergyNodeType.CABLE,
                    EnergyTier.LOW,
                    0L,
                    0L));
        }
    }

    @Benchmark
    public void buildGraph500Nodes(Blackhole bh) {
        NetworkGraph graph = new NetworkGraph();
        for (SimpleEnergyNode n : nodes) {
            graph.addNode(n);
        }
        bh.consume(graph.networks());
    }
}
