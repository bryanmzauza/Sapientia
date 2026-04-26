package dev.brmz.sapientia.benchmarks;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.brmz.sapientia.core.scheduler.SapientiaScheduler;
import dev.brmz.sapientia.core.tick.TickBucketing;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * P-007 — TickBucketing dispatch: 20 000 tickables split across the 20 rotating
 * buckets. Measures the cost of one bucket dispatch via the benchmark-only
 * {@code runOneTickForBenchmark} hook.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class TickBucketBenchmark {

    private static final int TICKABLE_COUNT = 20_000;

    private TickBucketing bucketing;

    @Setup
    public void setUp() {
        bucketing = new TickBucketing(Logger.getLogger("bench"), new NoopScheduler());
        for (int i = 0; i < TICKABLE_COUNT; i++) {
            final UUID id = new UUID(0L, i);
            bucketing.register(new TickBucketing.Tickable() {
                @Override public @NotNull UUID tickId() { return id; }
                @Override public void tick(long serverTick) { /* noop */ }
            });
        }
    }

    @Benchmark
    public void dispatchOneBucket() {
        bucketing.runOneTickForBenchmark();
    }

    /** Non-executing scheduler stub; the benchmark drives ticks manually. */
    private static final class NoopScheduler implements SapientiaScheduler {
        private static final CancellableTask NOOP = new CancellableTask() {
            @Override public void cancel() {}
            @Override public boolean isCancelled() { return true; }
        };
        @Override public void run(@NotNull Runnable task) {}
        @Override public void runAt(@NotNull Location location, @NotNull Runnable task) {}
        @Override public void runFor(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {}
        @Override public @NotNull CancellableTask repeat(@NotNull Runnable task, long delayTicks, long periodTicks) { return NOOP; }
        @Override public @NotNull CancellableTask runAsync(@NotNull Runnable task) { return NOOP; }
        @Override public @NotNull CancellableTask repeatAsync(@NotNull Runnable task, long delayTicks, long periodTicks) { return NOOP; }
        @Override public void shutdown() {}
    }
}
