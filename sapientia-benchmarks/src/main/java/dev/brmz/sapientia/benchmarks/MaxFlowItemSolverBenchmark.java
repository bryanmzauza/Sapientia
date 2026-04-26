package dev.brmz.sapientia.benchmarks;

import java.util.concurrent.TimeUnit;

import dev.brmz.sapientia.core.logistics.MaxFlowItemSolver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
 * P-019 (T-447 / 1.8.1) — Edmonds-Karp max-flow on a 1000-node HV item
 * network. Synthetic capacity matrix laid out as a {@code k}-wide grid so
 * the worst-case BFS still finishes inside the per-tick budget (≤ 2 ms).
 * The matrix is generated once in {@link #setUp()} and reused per
 * iteration; the residual graph is reset internally on every {@code maxFlow}
 * call so the measurement is self-contained.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MaxFlowItemSolverBenchmark {

    /** Total node count across all benchmark variants. */
    @Param({"100", "1000"})
    public int n;

    private long[][] capacity;
    private int sink;

    @Setup
    public void setUp() {
        // Grid-style network: every node connects to the next 4 (forward
        // chain). Bottleneck appears on alternating columns to give the BFS
        // multiple augmenting paths.
        capacity = new long[n][n];
        for (int i = 0; i < n - 1; i++) {
            int reach = Math.min(4, n - 1 - i);
            for (int k = 1; k <= reach; k++) {
                capacity[i][i + k] = (i % 5 == 0) ? 8L : 16L;
            }
        }
        this.sink = n - 1;
    }

    @Benchmark
    public void maxFlow1000Nodes(Blackhole bh) {
        MaxFlowItemSolver solver = new MaxFlowItemSolver(capacity);
        long flow = solver.maxFlow(0, sink);
        bh.consume(flow);
    }
}
