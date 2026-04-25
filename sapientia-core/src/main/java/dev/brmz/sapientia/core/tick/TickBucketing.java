package dev.brmz.sapientia.core.tick;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.core.scheduler.SapientiaScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Distributes ticking work across {@value #BUCKET_COUNT} rotating buckets so the cost of
 * each server tick stays bounded even as the number of machines grows. Each tickable is
 * assigned to a fixed bucket based on its id; every server tick the dispatcher runs the
 * bucket matching {@code currentTick % BUCKET_COUNT}. See ADR-006.
 */
public final class TickBucketing {

    public static final int BUCKET_COUNT = 20;

    private final Logger logger;
    private final SapientiaScheduler scheduler;
    private final ConcurrentMap<UUID, Entry> registry = new ConcurrentHashMap<>();
    private final List<List<Entry>> buckets = new ArrayList<>(BUCKET_COUNT);
    private SapientiaScheduler.CancellableTask runningTask;
    private long tickCounter;

    public TickBucketing(@NotNull Logger logger, @NotNull SapientiaScheduler scheduler) {
        this.logger = logger;
        this.scheduler = scheduler;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    /** Registers a {@link Tickable}. Returns the bucket it was assigned to. */
    public int register(@NotNull Tickable tickable) {
        UUID id = tickable.tickId();
        int bucket = Math.floorMod(id.hashCode(), BUCKET_COUNT);
        Entry entry = new Entry(id, bucket, tickable);
        if (registry.putIfAbsent(id, entry) != null) {
            return bucket;
        }
        synchronized (buckets) {
            buckets.get(bucket).add(entry);
        }
        return bucket;
    }

    public void unregister(@NotNull UUID id) {
        Entry entry = registry.remove(id);
        if (entry == null) {
            return;
        }
        synchronized (buckets) {
            buckets.get(entry.bucket).remove(entry);
        }
    }

    /** Starts the repeating tick dispatcher. Idempotent. */
    public void start() {
        if (runningTask != null) {
            return;
        }
        runningTask = scheduler.repeat(this::runOneTick, 1L, 1L);
    }

    public void stop() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
    }

    public int size() {
        return registry.size();
    }

    /**
     * Benchmark-only entry point that dispatches a single tick to the current
     * bucket without requiring the background scheduler (T-170 / 1.0.0-beta).
     * Runtime callers should use {@link #start()} instead.
     */
    public void runOneTickForBenchmark() {
        runOneTick();
    }

    private void runOneTick() {
        long tick = tickCounter++;
        int bucketIndex = (int) Math.floorMod(tick, BUCKET_COUNT);
        List<Entry> snapshot;
        synchronized (buckets) {
            snapshot = new ArrayList<>(buckets.get(bucketIndex));
        }
        for (Entry entry : snapshot) {
            try {
                entry.tickable.tick(tick);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Tickable " + entry.id + " threw during tick", e);
            }
        }
    }

    private record Entry(UUID id, int bucket, Tickable tickable) {}

    /** Anything wanting to run periodic logic via the tick dispatcher. */
    public interface Tickable {

        /** Stable identity used to bucket the tickable. */
        @NotNull UUID tickId();

        /**
         * Invoked on the server main thread (or region thread on Folia).
         * @param serverTick monotonically increasing counter owned by the dispatcher
         */
        void tick(long serverTick);
    }
}
