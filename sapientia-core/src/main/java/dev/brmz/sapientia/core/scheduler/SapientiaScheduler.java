package dev.brmz.sapientia.core.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified scheduling facade that routes to {@code BukkitScheduler} on Paper and to
 * {@code RegionScheduler} / {@code EntityScheduler} on Folia. See ADR-005.
 *
 * <p>Folia is detected once at creation via reflection on
 * {@code io.papermc.paper.threadedregions.RegionizedServer}. Every call is thread-safe.
 */
public interface SapientiaScheduler {

    /** Creates the adapter appropriate for the current server. */
    static @NotNull SapientiaScheduler create(@NotNull Plugin plugin) {
        boolean folia = isFolia();
        plugin.getLogger().info(() -> folia
                ? "Folia detected; using RegionScheduler + EntityScheduler adapters."
                : "Running on Paper; using BukkitScheduler adapter.");
        return folia ? new FoliaScheduler(plugin) : new BukkitSchedulerAdapter(plugin);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Runs a task on the main/region thread as soon as the current tick allows. */
    void run(@NotNull Runnable task);

    /** Runs a task tied to a location's region (main thread on Paper). */
    void runAt(@NotNull Location location, @NotNull Runnable task);

    /** Runs a task tied to an entity (entity scheduler on Folia). */
    void runFor(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);

    /** Runs a repeating region/main task. Returns a handle usable for cancellation. */
    @NotNull CancellableTask repeat(@NotNull Runnable task, long delayTicks, long periodTicks);

    /** Runs on an async worker thread. */
    @NotNull CancellableTask runAsync(@NotNull Runnable task);

    /** Runs periodically on an async worker thread. */
    @NotNull CancellableTask repeatAsync(@NotNull Runnable task, long delayTicks, long periodTicks);

    /** Cancels all scheduled work and releases internal references. */
    void shutdown();

    /** Handle representing a running scheduled task. */
    interface CancellableTask {
        void cancel();

        boolean isCancelled();
    }
}
