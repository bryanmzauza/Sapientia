package dev.brmz.sapientia.benchmarks;

import java.util.concurrent.TimeUnit;

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
 * P-020 (T-459 / 1.9.0) — placeholder for the android tick-budget
 * benchmark. The real harness lands in 1.9.1 once the kinetic loop
 * exists; this entry is a synthetic snapshot iteration that anchors the
 * benchmark module + JMH harness wiring so the 1.9.1 PR can drop a
 * concrete implementation in without infrastructure churn.
 *
 * <p>The {@code AndroidServiceImpl#snapshot()} cost in 1.9.0 is dominated
 * by a {@code ConcurrentHashMap.values()} traversal — this benchmark
 * approximates it with a synthetic {@code long[]} sweep proportional to
 * the configured population.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AndroidTickBenchmark {

    /** Population sizes exercised by the placeholder. */
    @Param({"100", "200"})
    public int androids;

    private long[] payload;

    @Setup
    public void setUp() {
        payload = new long[androids];
        for (int i = 0; i < androids; i++) {
            // Stable deterministic seed; avoids JMH dead-code elimination
            // collapsing the inner loop.
            payload[i] = ((long) i << 17) ^ 0xDEADBEEFL;
        }
    }

    /**
     * Approximates the per-tick snapshot iteration. Replaced in 1.9.1 with
     * the real {@code AndroidTicker#tick} call once the kinetic loop is
     * implemented.
     */
    @Benchmark
    public void snapshotSweep(Blackhole bh) {
        long acc = 0L;
        for (long v : payload) {
            acc ^= v;
        }
        bh.consume(acc);
    }
}
