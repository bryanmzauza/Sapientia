package dev.brmz.sapientia.benchmarks;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.brmz.sapientia.core.engine.BlockPositions;
import dev.brmz.sapientia.core.engine.MachineScheduler;
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
 * Foundation 1 scale benchmark: cost of one scheduler tick with up to
 * 10 million registered machines.
 *
 * <p>Every machine works on a 200-tick cycle (10 s), so each tick runs
 * {@code machines / 200} of them: 50,000 per tick at 10 million. The behaviour
 * does trivial work, so the score is the scheduler's own overhead per tick,
 * which must stay well inside the 5 ms budget. Machines that are not due cost
 * nothing, so the score grows with the machines run per tick, not with the
 * total registered.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
@Fork(value = 1, jvmArgsAppend = "-Xmx4g")
@State(Scope.Benchmark)
public class MachineSchedulerBenchmark {

    private static final int CYCLE_TICKS = 200;

    @Param({"100000", "1000000", "10000000"})
    public int machines;

    private MachineScheduler scheduler;
    private long tick;
    private long work;

    @Setup(Level.Trial)
    public void setUp() {
        scheduler = new MachineScheduler(Logger.getLogger("bench"));
        int behavior = scheduler.addBehavior(context -> {
            work += context.position(); // stand-in for real machine work
            return CYCLE_TICKS;
        });
        for (int i = 0; i < machines; i++) {
            scheduler.register(i & 7, BlockPositions.pack(i & 0xFFFF, 64, i >> 16), behavior,
                    1 + (i % CYCLE_TICKS));
        }
        // Advance one full cycle so every machine sits at its steady-state slot.
        for (int t = 1; t <= CYCLE_TICKS; t++) {
            scheduler.tick(++tick, Long.MAX_VALUE);
        }
    }

    @Benchmark
    public int oneTick(Blackhole blackhole) {
        int ran = scheduler.tick(++tick, Long.MAX_VALUE);
        blackhole.consume(work);
        return ran;
    }
}
