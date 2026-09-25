package dev.brmz.sapientia.benchmarks;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import dev.brmz.sapientia.api.android.AndroidType;
import dev.brmz.sapientia.core.android.AndroidLootTables;
import dev.brmz.sapientia.core.android.AndroidUpgradeScaling;
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
 * P-020 (T-459 / 1.9.1) — kinetic-loop tick budget.
 *
 * <p>Measures the CPU cost of the {@code AndroidTicker} hot path for a
 * synthetic snapshot of {@code N} androids. Specifically, per simulated
 * android per measured tick:
 * <ol>
 *   <li>Look up {@link AndroidUpgradeScaling#motorCooldownTicks(int)} for
 *       the cooldown gate.</li>
 *   <li>Roll one {@link AndroidLootTables#roll(AndroidType, long)} when the
 *       cooldown elapsed (the dominant non-IO cost in the real engine —
 *       chest reads/writes are OS-bound and explicitly outside the
 *       {@code performance-contract.md} budget envelope).</li>
 * </ol>
 *
 * <p>The budget contract is "200 androids fit in the per-tick wall budget
 * at ≥ 18 TPS", i.e. ≤ 50 ms of pure CPU per tick. Bukkit IO is faulted
 * separately by the benchmarks in {@code TickBucketBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AndroidTickBenchmark {

    /** Population sizes exercised by the harness — covers the P-020 cap of 200. */
    @Param({"100", "200"})
    public int androids;

    private AndroidType[] types;
    private int[] motorTiers;
    private long[] cooldownUntil;
    private long tickCount;

    @Setup
    public void setUp() {
        AndroidType[] all = AndroidType.values();
        types = new AndroidType[androids];
        motorTiers = new int[androids];
        cooldownUntil = new long[androids];
        Random r = new Random(0xC0FFEEL);
        for (int i = 0; i < androids; i++) {
            types[i] = all[r.nextInt(all.length)];
            motorTiers[i] = 1 + r.nextInt(4);
            cooldownUntil[i] = 0L;
        }
        tickCount = 0L;
    }

    /**
     * Runs one simulated tick over the synthetic snapshot. Mirrors the
     * non-IO portion of {@code AndroidTicker#tick} verbatim so changes to
     * the scaling table or loot weights are reflected in the regression
     * watchdog (T-171 / baseline.json).
     */
    @Benchmark
    public void tick(Blackhole bh) {
        long t = ++tickCount;
        for (int i = 0; i < androids; i++) {
            if (t < cooldownUntil[i]) continue;
            AndroidLootTables.LootDrop drop = AndroidLootTables.roll(types[i], (long) i ^ t);
            bh.consume(drop);
            cooldownUntil[i] = t + AndroidUpgradeScaling.motorCooldownTicks(motorTiers[i]);
        }
    }
}
