package dev.brmz.sapientia.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * P-012 — overhead with 20% Bedrock vs. 100% Java players. Full Spark
 * comparison runs against a real server; this microbench simulates the per-
 * player branch we add for Bedrock detection on every tick (an extra hash
 * lookup) so we can spot regressions in the cheap path. Target: the {@code
 * mixed20} variant must stay within 2% of {@code allJava} on average.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class BedrockMixOverheadBenchmark {

    private static final int PLAYERS = 50;

    @Benchmark
    public long allJava() {
        long acc = 0;
        for (int i = 0; i < PLAYERS; i++) {
            acc += javaPath(i);
        }
        return acc;
    }

    @Benchmark
    public long mixed20() {
        long acc = 0;
        for (int i = 0; i < PLAYERS; i++) {
            // 20% Bedrock — every 5th player.
            acc += (i % 5 == 0) ? bedrockPath(i) : javaPath(i);
        }
        return acc;
    }

    private static long javaPath(int idx) {
        return idx * 31L + 7L;
    }

    private static long bedrockPath(int idx) {
        // Single extra hash lookup + branch for Bedrock players.
        long h = idx * 17L;
        h ^= (h >>> 16);
        return h + 13L;
    }
}
