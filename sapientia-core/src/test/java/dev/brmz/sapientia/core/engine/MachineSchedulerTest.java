package dev.brmz.sapientia.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class MachineSchedulerTest {

    private static final Logger LOG = Logger.getLogger("MachineSchedulerTest");
    private static final long NO_LIMIT = Long.MAX_VALUE;

    private final AtomicLong clock = new AtomicLong();
    private final MachineScheduler scheduler = new MachineScheduler(LOG, clock::get);

    /** Records which ticks each handle ran on. */
    private final List<long[]> runs = new ArrayList<>();

    private int behaviorReturning(int result) {
        return scheduler.addBehavior(ctx -> {
            runs.add(new long[] {ctx.handle(), ctx.tick()});
            return result;
        });
    }

    private void runTicks(long from, long to) {
        for (long t = from; t <= to; t++) {
            scheduler.tick(t, NO_LIMIT);
        }
    }

    private List<Long> ticksOf(int handle) {
        return runs.stream().filter(r -> r[0] == handle).map(r -> r[1]).toList();
    }

    @Test
    void machineRunsOnlyWhenDueAndThenEveryPeriod() {
        int behavior = behaviorReturning(5);
        int handle = scheduler.register(0, BlockPositions.pack(1, 64, 1), behavior, 3);

        runTicks(1, 20);

        assertThat(ticksOf(handle)).containsExactly(3L, 8L, 13L, 18L);
    }

    @Test
    void idleMachinesBackOffExponentiallyUpToTheCap() {
        int behavior = behaviorReturning(MachineBehavior.idle(10));
        int handle = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 1000);

        List<Long> ticks = ticksOf(handle);
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < ticks.size(); i++) gaps.add(ticks.get(i) - ticks.get(i - 1));
        assertThat(gaps).startsWith(10L, 20L, 40L, 80L, 160L, 200L, 200L);
    }

    @Test
    void workingResetsTheBackOff() {
        int[] calls = {0};
        int behavior = scheduler.addBehavior(ctx -> {
            runs.add(new long[] {ctx.handle(), ctx.tick()});
            calls[0]++;
            return calls[0] <= 3 ? MachineBehavior.idle(10) : 5;
        });
        int handle = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 100);

        // idle at 1 (+10), 11 (+20), 31 (+40), then works at 71 and every 5 ticks.
        assertThat(ticksOf(handle)).startsWith(1L, 11L, 31L, 71L, 76L, 81L);
    }

    @Test
    void sleepingMachinesCostNothingUntilWoken() {
        int behavior = behaviorReturning(MachineBehavior.SLEEP);
        int handle = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 50);
        assertThat(ticksOf(handle)).containsExactly(1L);
        assertThat(scheduler.stateOf(handle)).isEqualTo(MachineScheduler.SLEEPING);

        scheduler.wake(handle);
        runTicks(51, 60);
        assertThat(ticksOf(handle)).containsExactly(1L, 51L);
    }

    @Test
    void wakeBringsAScheduledMachineForward() {
        int behavior = behaviorReturning(100);
        int handle = scheduler.register(0, 0L, behavior, 1);
        runTicks(1, 10);

        scheduler.wake(handle);
        runTicks(11, 12);

        assertThat(ticksOf(handle)).containsExactly(1L, 11L);
    }

    @Test
    void removeUnregistersAndFreesTheHandle() {
        int behavior = behaviorReturning(MachineBehavior.REMOVE);
        int handle = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 10);

        assertThat(ticksOf(handle)).containsExactly(1L);
        assertThat(scheduler.registeredCount()).isZero();
        assertThat(scheduler.register(0, 0L, behavior, 1)).isEqualTo(handle);
    }

    @Test
    void unregisterFromInsideTheBehaviourIsSafe() {
        int[] self = new int[1];
        int behavior = scheduler.addBehavior(ctx -> {
            scheduler.unregister(ctx.handle());
            return 5;
        });
        self[0] = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 20);

        assertThat(scheduler.registeredCount()).isZero();
    }

    @Test
    void pausedMachinesDoNotRunUntilResumed() {
        int behavior = behaviorReturning(5);
        int handle = scheduler.register(0, 0L, behavior, 1);
        runTicks(1, 1);

        scheduler.pause(handle);
        runTicks(2, 30);
        assertThat(ticksOf(handle)).containsExactly(1L);

        scheduler.resume(handle, 2);
        runTicks(31, 40);
        assertThat(ticksOf(handle)).containsExactly(1L, 32L, 37L);
    }

    @Test
    void budgetLeavesLateMachinesForTheNextTick() {
        int behavior = scheduler.addBehavior(ctx -> {
            clock.addAndGet(1_000); // each run "costs" 1 µs
            runs.add(new long[] {ctx.handle(), ctx.tick()});
            return 100;
        });
        for (int i = 0; i < 200; i++) {
            scheduler.register(0, 0L, behavior, 1);
        }

        int first = scheduler.tick(1, 64_000); // budget for 64 runs
        assertThat(first).isEqualTo(64);
        assertThat(scheduler.lastTickLate()).isEqualTo(136);

        int second = scheduler.tick(2, Long.MAX_VALUE);
        assertThat(second).isEqualTo(136);
        assertThat(scheduler.readyBacklog()).isZero();
    }

    @Test
    void skippedTicksDoNotLoseMachines() {
        int behavior = behaviorReturning(MachineBehavior.REMOVE);
        int handle = scheduler.register(0, 0L, behavior, 7);

        scheduler.tick(20, NO_LIMIT); // jumped over tick 7

        assertThat(ticksOf(handle)).containsExactly(20L);
    }

    @Test
    void failingBehaviourBacksOffInsteadOfCrashing() {
        int behavior = scheduler.addBehavior(ctx -> {
            runs.add(new long[] {ctx.handle(), ctx.tick()});
            throw new IllegalStateException("boom");
        });
        int handle = scheduler.register(0, 0L, behavior, 1);

        runTicks(1, 300);

        assertThat(ticksOf(handle)).containsExactly(1L, 201L);
    }

    @Test
    void growsBeyondInitialCapacity() {
        int behavior = behaviorReturning(MachineBehavior.REMOVE);
        for (int i = 0; i < 5000; i++) {
            scheduler.register(0, BlockPositions.pack(i, 0, i), behavior, 1 + (i % 50));
        }
        assertThat(scheduler.registeredCount()).isEqualTo(5000);

        runTicks(1, 60);

        assertThat(runs).hasSize(5000);
        assertThat(scheduler.registeredCount()).isZero();
    }
}
