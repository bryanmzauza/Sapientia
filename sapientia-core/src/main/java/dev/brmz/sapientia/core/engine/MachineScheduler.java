package dev.brmz.sapientia.core.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

/**
 * Event-driven machine scheduler built on a timing wheel.
 *
 * <p>Each registered machine is an {@code int} handle whose state lives in
 * parallel primitive arrays, so millions of machines cost a few dozen bytes
 * each and no per-machine objects. A machine only costs CPU on the tick its
 * next run is due: the scheduler never scans idle or future machines.
 *
 * <p>Per tick, {@link #tick} moves the machines due now into a ready queue
 * and runs them until the time budget is spent. Anything left over stays in
 * the queue and runs first on the next tick, so under extreme load machines
 * slow down instead of the server stalling.
 *
 * <p>A behaviour's return value decides what happens next (see
 * {@link MachineBehavior}): run again after a delay, back off while idle,
 * sleep until {@link #wake} is called, or unregister.
 *
 * <p>Not thread-safe: call everything from the thread that owns the tick.
 */
public final class MachineScheduler {

    /** Wheel size in ticks; delays are clamped to {@code WHEEL_SIZE - 1}. */
    public static final int WHEEL_SIZE = 4096;
    /** Longest back-off an idle machine reaches (10 s). */
    public static final int MAX_IDLE_DELAY = 200;

    private static final int WHEEL_MASK = WHEEL_SIZE - 1;
    private static final int NONE = -1;
    private static final int BUDGET_CHECK_INTERVAL = 32;

    static final byte FREE = 0;
    static final byte SCHEDULED = 1;
    static final byte READY = 2;
    static final byte RUNNING = 3;
    static final byte SLEEPING = 4;
    static final byte PAUSED = 5;

    private final Logger logger;
    private final LongSupplier clock;
    private final List<MachineBehavior> behaviors = new ArrayList<>();

    // Per-handle state (parallel arrays), 27 bytes per machine. Due ticks are
    // stored as ints and compared by difference, so they survive wrap-around.
    // Free handles are chained through next[].
    private long[] position = new long[0];
    private int[] due = new int[0];
    private int[] next = new int[0];
    private int[] prev = new int[0];
    private short[] world = new short[0];
    private short[] behavior = new short[0];
    private short[] backoff = new short[0];
    private byte[] state = new byte[0];

    private int freeHead = NONE;
    private int capacity;
    private int registered;

    private final int[] wheel = new int[WHEEL_SIZE];
    private final IntQueue ready = new IntQueue();
    private long currentTick;
    private long lastTickLate;
    private long lastTickRuns;
    private final MachineContext context = new MachineContext();

    public MachineScheduler(@NotNull Logger logger) {
        this(logger, System::nanoTime);
    }

    /** Visible for tests: lets the budget clock be simulated. */
    MachineScheduler(@NotNull Logger logger, @NotNull LongSupplier clock) {
        this.logger = logger;
        this.clock = clock;
        Arrays.fill(wheel, NONE);
        grow(1024);
    }

    /** Registers a behaviour and returns its id, used when registering machines. */
    public int addBehavior(@NotNull MachineBehavior machineBehavior) {
        if (behaviors.size() >= Short.MAX_VALUE) {
            throw new IllegalStateException("Too many machine behaviours");
        }
        behaviors.add(machineBehavior);
        return behaviors.size() - 1;
    }

    /**
     * Registers a machine and schedules its first run after {@code initialDelay}
     * ticks (at least 1). Returns the handle.
     */
    public int register(int worldId, long packedPosition, int behaviorId, int initialDelay) {
        if (behaviorId < 0 || behaviorId >= behaviors.size()) {
            throw new IllegalArgumentException("Unknown behaviour id " + behaviorId);
        }
        if (worldId < 0 || worldId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("World id out of range: " + worldId);
        }
        int handle = allocate();
        position[handle] = packedPosition;
        world[handle] = (short) worldId;
        behavior[handle] = (short) behaviorId;
        backoff[handle] = 0;
        schedule(handle, currentTick + Math.max(1, initialDelay));
        registered++;
        return handle;
    }

    /** Removes a machine. Safe to call from inside its own behaviour. */
    public void unregister(int handle) {
        if (!isLive(handle)) {
            return;
        }
        if (state[handle] == SCHEDULED) {
            unlink(handle);
        }
        state[handle] = FREE;
        next[handle] = freeHead;
        freeHead = handle;
        registered--;
    }

    /**
     * Runs a sleeping or idle machine on the next tick. Use when something the
     * machine waits for has changed (an item arrived, energy came back).
     */
    public void wake(int handle) {
        if (!isLive(handle)) {
            return;
        }
        backoff[handle] = 0;
        byte s = state[handle];
        if (s == SLEEPING) {
            schedule(handle, currentTick + 1);
        } else if (s == SCHEDULED && due[handle] - (int) (currentTick + 1) > 0) {
            unlink(handle);
            schedule(handle, currentTick + 1);
        }
    }

    /** Stops scheduling a machine until {@link #resume}; costs nothing while paused. */
    public void pause(int handle) {
        if (!isLive(handle)) {
            return;
        }
        if (state[handle] == SCHEDULED) {
            unlink(handle);
        }
        // READY entries are skipped when the queue reaches them.
        state[handle] = PAUSED;
    }

    /** Resumes a paused machine after {@code delay} ticks. */
    public void resume(int handle, int delay) {
        if (isLive(handle) && state[handle] == PAUSED) {
            schedule(handle, currentTick + Math.max(1, delay));
        }
    }

    /**
     * Advances to {@code tick} and runs due machines until {@code budgetNanos}
     * is spent. Returns the number of machines run.
     */
    public int tick(long tick, long budgetNanos) {
        // Collect every slot passed since the last call, so skipped ticks never lose machines.
        long from = Math.max(currentTick + 1, tick - WHEEL_SIZE + 1);
        if (tick <= currentTick) {
            from = tick;
        }
        currentTick = tick;
        for (long t = from; t <= tick; t++) {
            collectDue((int) (t & WHEEL_MASK), tick);
        }

        long start = clock.getAsLong();
        int runs = 0;
        int late = 0;
        while (!ready.isEmpty()) {
            if (runs > 0 && runs % BUDGET_CHECK_INTERVAL == 0
                    && clock.getAsLong() - start >= budgetNanos) {
                late = ready.size();
                break;
            }
            int handle = ready.poll();
            if (state[handle] != READY) {
                continue; // paused, unregistered or rescheduled while waiting
            }
            run(handle);
            runs++;
        }
        lastTickRuns = runs;
        lastTickLate = late;
        return runs;
    }

    private void collectDue(int slot, long tick) {
        int h = wheel[slot];
        wheel[slot] = NONE;
        while (h != NONE) {
            int following = next[h];
            if (due[h] - (int) tick <= 0) {
                state[h] = READY;
                ready.add(h);
            } else {
                linkInto(slot, h); // due on a later lap of the wheel
            }
            h = following;
        }
    }

    private void run(int handle) {
        state[handle] = RUNNING;
        context.bind(handle, world[handle], position[handle], currentTick);
        int result;
        try {
            result = behaviors.get(behavior[handle]).run(context);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Machine behaviour failed at "
                    + BlockPositions.describe(position[handle]) + "; backing off", e);
            result = MachineBehavior.idle(MAX_IDLE_DELAY);
        }
        if (state[handle] != RUNNING) {
            return; // unregistered or paused by its own behaviour
        }
        if (result == MachineBehavior.REMOVE) {
            unregister(handle);
        } else if (result == MachineBehavior.SLEEP) {
            state[handle] = SLEEPING;
        } else if (result > 0) {
            backoff[handle] = 0;
            schedule(handle, currentTick + result);
        } else {
            int base = -result;
            int delay = backoff[handle] == 0 ? base : Math.min(MAX_IDLE_DELAY, backoff[handle] * 2);
            backoff[handle] = (short) Math.min(Math.max(delay, base), MAX_IDLE_DELAY);
            schedule(handle, currentTick + backoff[handle]);
        }
    }

    // --- Wheel -------------------------------------------------------------------

    private void schedule(int handle, long dueTick) {
        long clamped = Math.min(dueTick, currentTick + WHEEL_SIZE - 1);
        due[handle] = (int) clamped;
        state[handle] = SCHEDULED;
        linkInto((int) (clamped & WHEEL_MASK), handle);
    }

    private void linkInto(int slot, int handle) {
        int head = wheel[slot];
        next[handle] = head;
        prev[handle] = NONE;
        if (head != NONE) {
            prev[head] = handle;
        }
        wheel[slot] = handle;
    }

    private void unlink(int handle) {
        int slot = due[handle] & WHEEL_MASK;
        int p = prev[handle];
        int n = next[handle];
        if (p != NONE) {
            next[p] = n;
        } else {
            wheel[slot] = n;
        }
        if (n != NONE) {
            prev[n] = p;
        }
        next[handle] = NONE;
        prev[handle] = NONE;
    }

    // --- Storage -----------------------------------------------------------------

    private int allocate() {
        if (freeHead == NONE) {
            grow(capacity + (capacity >> 1)); // 1.5x keeps unused capacity low at millions of machines
        }
        int handle = freeHead;
        freeHead = next[handle];
        return handle;
    }

    private void grow(int newCapacity) {
        int old = capacity;
        position = Arrays.copyOf(position, newCapacity);
        due = Arrays.copyOf(due, newCapacity);
        next = Arrays.copyOf(next, newCapacity);
        prev = Arrays.copyOf(prev, newCapacity);
        world = Arrays.copyOf(world, newCapacity);
        behavior = Arrays.copyOf(behavior, newCapacity);
        backoff = Arrays.copyOf(backoff, newCapacity);
        state = Arrays.copyOf(state, newCapacity);
        // Chain new handles so the lowest ones are handed out first.
        for (int h = newCapacity - 1; h >= old; h--) {
            next[h] = freeHead;
            freeHead = h;
        }
        capacity = newCapacity;
    }

    private boolean isLive(int handle) {
        return handle >= 0 && handle < capacity && state[handle] != FREE;
    }

    // --- Introspection ---------------------------------------------------------------

    public int registeredCount() { return registered; }

    /** Packed block position of a registered machine. */
    public long position(int handle) { return position[handle]; }

    public int readyBacklog() { return ready.size(); }

    public long lastTickRuns() { return lastTickRuns; }

    /** Machines that were due but did not fit in the last tick's budget. */
    public long lastTickLate() { return lastTickLate; }

    /** Counts handles in a given state; O(capacity), meant for diagnostics only. */
    public int countInState(byte wanted) {
        int n = 0;
        for (int h = 0; h < capacity; h++) {
            if (state[h] == wanted) n++;
        }
        return n;
    }

    byte stateOf(int handle) { return state[handle]; }

    long dueOf(int handle) { return due[handle]; }

    /** Growable ring buffer of ints. */
    static final class IntQueue {
        private int[] items = new int[256];
        private int head;
        private int size;

        void add(int value) {
            if (size == items.length) {
                int[] bigger = new int[items.length * 2];
                for (int i = 0; i < size; i++) {
                    bigger[i] = items[(head + i) % items.length];
                }
                items = bigger;
                head = 0;
            }
            items[(head + size) % items.length] = value;
            size++;
        }

        int poll() {
            int value = items[head];
            head = (head + 1) % items.length;
            size--;
            return value;
        }

        boolean isEmpty() { return size == 0; }

        int size() { return size; }
    }
}
