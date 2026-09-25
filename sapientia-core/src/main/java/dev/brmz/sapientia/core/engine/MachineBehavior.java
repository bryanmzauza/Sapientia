package dev.brmz.sapientia.core.engine;

import org.jetbrains.annotations.NotNull;

/**
 * What a machine does when the scheduler runs it. One behaviour instance
 * serves every machine of its type; per-machine data is reached through the
 * {@link MachineContext}.
 *
 * <p>The return value tells the scheduler what to do next:
 * <ul>
 *   <li>a positive number: the machine worked; run it again after that many ticks;</li>
 *   <li>{@link #idle(int)}: nothing to do; back off, starting at the given delay and
 *       doubling up to {@link MachineScheduler#MAX_IDLE_DELAY} until it works again;</li>
 *   <li>{@link #SLEEP}: stop until {@link MachineScheduler#wake} is called;</li>
 *   <li>{@link #REMOVE}: unregister the machine.</li>
 * </ul>
 */
@FunctionalInterface
public interface MachineBehavior {

    int REMOVE = 0;
    int SLEEP = Integer.MIN_VALUE;

    int run(@NotNull MachineContext context);

    /** Idle result with the given starting back-off (ticks, at least 1). */
    static int idle(int baseDelay) {
        return -Math.max(1, Math.min(baseDelay, MachineScheduler.MAX_IDLE_DELAY));
    }
}
