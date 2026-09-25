package dev.brmz.sapientia.core.persistence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

/**
 * The single background thread that talks to SQLite. Every
 * {@link #FLUSH_INTERVAL_MS} ms it flushes the registered write queues in one
 * pass; tasks submitted through {@link #execute} (chunk reads, filter rule
 * edits) flush first, so they always come after every write queued before them. The main thread never waits on
 * the database.
 */
public final class DatabaseWorker {

    /** How often queued writes reach the database, in milliseconds. */
    public static final long FLUSH_INTERVAL_MS = 500L;

    /** A write queue flushed by this worker. {@link #flush} must be safe to call from any thread. */
    public interface Flushable {
        void flush();
    }

    private final Logger logger;
    private final List<Flushable> queues = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService executor;

    public DatabaseWorker(@NotNull Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sapientia-Database");
            t.setDaemon(true);
            return t;
        });
    }

    public void register(@NotNull Flushable queue) {
        queues.add(queue);
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::flushAll, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs {@code task} on the database thread after flushing queued writes, so
     * it sees (and comes after) every write queued before it.
     */
    public void execute(@NotNull Runnable task) {
        if (executor.isShutdown()) {
            return; // the plugin is stopping; nothing will apply the result
        }
        try {
            executor.execute(() -> {
                flushAll();
                try {
                    task.run();
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "Database task failed", e);
                }
            });
        } catch (RejectedExecutionException e) {
            // Shut down between the check and the submit: same as above.
        }
    }

    /** Flushes every registered queue on the calling thread. */
    public void flushAll() {
        for (Flushable queue : queues) {
            try {
                queue.flush();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Database flush failed", e);
            }
        }
    }

    /** Stops the thread, then writes whatever is still queued. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {
                logger.warning("Database thread did not stop in time; flushing from the main thread.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        flushAll();
    }
}
