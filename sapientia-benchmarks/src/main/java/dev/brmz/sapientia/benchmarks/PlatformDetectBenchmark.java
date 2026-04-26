package dev.brmz.sapientia.benchmarks;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

/**
 * P-009 / P-010 — platform detection.
 *
 * <p>{@link #cacheHit()} measures a per-UUID cache lookup against a populated
 * {@link ConcurrentHashMap} (target &lt; 1 ms). {@link #cacheMiss()} measures
 * the cold path that, in production, queries Floodgate via reflection and
 * caches the result (target &lt; 5 ms — simulated here with a short busy
 * loop because the real Floodgate API is not part of the benchmark module's
 * runtime classpath).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class PlatformDetectBenchmark {

    private final ConcurrentHashMap<UUID, Boolean> cache = new ConcurrentHashMap<>();
    private final UUID hitKey = new UUID(1L, 1L);

    @Setup
    public void setUp() {
        cache.put(hitKey, Boolean.TRUE);
    }

    @Benchmark
    public Boolean cacheHit() {
        return cache.get(hitKey);
    }

    @Benchmark
    public Boolean cacheMiss() {
        UUID key = UUID.randomUUID();
        return cache.computeIfAbsent(key, k -> simulateFloodgateProbe());
    }

    /** Stand-in for the real Floodgate reflection probe (P-010 contract). */
    private static Boolean simulateFloodgateProbe() {
        long acc = 0;
        for (int i = 0; i < 256; i++) {
            acc += i * 31L;
        }
        return acc < 0;
    }
}
