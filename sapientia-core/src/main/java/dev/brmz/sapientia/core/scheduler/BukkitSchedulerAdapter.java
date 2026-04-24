package dev.brmz.sapientia.core.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** {@link SapientiaScheduler} implementation backed by the classic {@code BukkitScheduler}. */
final class BukkitSchedulerAdapter implements SapientiaScheduler {

    private final Plugin plugin;

    BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(@NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAt(@NotNull Location location, @NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runFor(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (entity.isValid()) {
                task.run();
            } else if (retired != null) {
                retired.run();
            }
        });
    }

    @Override
    public @NotNull CancellableTask repeat(@NotNull Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return new BukkitHandle(bt);
    }

    @Override
    public @NotNull CancellableTask runAsync(@NotNull Runnable task) {
        BukkitTask bt = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return new BukkitHandle(bt);
    }

    @Override
    public @NotNull CancellableTask repeatAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return new BukkitHandle(bt);
    }

    @Override
    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private record BukkitHandle(BukkitTask task) implements CancellableTask {
        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
