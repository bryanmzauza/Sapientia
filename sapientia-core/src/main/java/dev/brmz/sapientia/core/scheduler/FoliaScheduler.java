package dev.brmz.sapientia.core.scheduler;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Folia-aware scheduler adapter. Uses reflection so the plugin keeps compiling against
 * the Paper API only; falls back to global scheduling if Folia APIs are absent.
 */
final class FoliaScheduler implements SapientiaScheduler {

    private final Plugin plugin;
    private final Object globalRegionScheduler;
    private final Object asyncScheduler;
    private final Method globalExecute;
    private final Method globalRunAtFixedRate;
    private final Method regionExecute;
    private final Method regionRunAtFixedRate;
    private final Method entityRun;
    private final Method asyncRunNow;
    private final Method asyncRunAtFixedRate;

    FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        try {
            Object server = Bukkit.getServer();
            this.globalRegionScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
            this.asyncScheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
            Class<?> rs = server.getClass().getMethod("getRegionScheduler").invoke(server).getClass();

            this.globalExecute = globalRegionScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            this.globalRunAtFixedRate = globalRegionScheduler.getClass().getMethod(
                    "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class);

            this.regionExecute = rs.getMethod("execute", Plugin.class,
                    org.bukkit.World.class, int.class, int.class, Runnable.class);
            this.regionRunAtFixedRate = rs.getMethod("runAtFixedRate", Plugin.class,
                    org.bukkit.World.class, int.class, int.class,
                    java.util.function.Consumer.class, long.class, long.class);

            Class<?> entityScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            this.entityRun = entityScheduler.getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class);

            this.asyncRunNow = asyncScheduler.getClass().getMethod(
                    "runNow", Plugin.class, java.util.function.Consumer.class);
            this.asyncRunAtFixedRate = asyncScheduler.getClass().getMethod(
                    "runAtFixedRate", Plugin.class, java.util.function.Consumer.class,
                    long.class, long.class, TimeUnit.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Folia APIs missing or mismatched", e);
        }
    }

    @Override
    public void run(@NotNull Runnable task) {
        invoke(() -> globalExecute.invoke(globalRegionScheduler, plugin, task));
    }

    @Override
    public void runAt(@NotNull Location location, @NotNull Runnable task) {
        invoke(() -> regionExecute.invoke(
                Bukkit.getServer().getClass().getMethod("getRegionScheduler").invoke(Bukkit.getServer()),
                plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task));
    }

    @Override
    public void runFor(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            java.util.function.Consumer<Object> consumer = ignored -> task.run();
            entityRun.invoke(scheduler, plugin, consumer, retired);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Folia entity scheduling failed: " + e.getMessage());
        }
    }

    @Override
    public @NotNull CancellableTask repeat(@NotNull Runnable task, long delayTicks, long periodTicks) {
        AtomicReference<Object> handle = new AtomicReference<>();
        invoke(() -> {
            java.util.function.Consumer<Object> consumer = ignored -> task.run();
            handle.set(globalRunAtFixedRate.invoke(globalRegionScheduler, plugin, consumer,
                    Math.max(1, delayTicks), Math.max(1, periodTicks)));
            return null;
        });
        return new ScheduledTaskHandle(handle.get());
    }

    @Override
    public @NotNull CancellableTask runAsync(@NotNull Runnable task) {
        AtomicReference<Object> handle = new AtomicReference<>();
        invoke(() -> {
            java.util.function.Consumer<Object> consumer = ignored -> task.run();
            handle.set(asyncRunNow.invoke(asyncScheduler, plugin, consumer));
            return null;
        });
        return new ScheduledTaskHandle(handle.get());
    }

    @Override
    public @NotNull CancellableTask repeatAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        AtomicReference<Object> handle = new AtomicReference<>();
        invoke(() -> {
            java.util.function.Consumer<Object> consumer = ignored -> task.run();
            long delayMs = delayTicks * 50L;
            long periodMs = periodTicks * 50L;
            handle.set(asyncRunAtFixedRate.invoke(asyncScheduler, plugin, consumer,
                    Math.max(1, delayMs), Math.max(1, periodMs), TimeUnit.MILLISECONDS));
            return null;
        });
        return new ScheduledTaskHandle(handle.get());
    }

    @Override
    public void shutdown() {
        try {
            Method cancel = globalRegionScheduler.getClass().getMethod("cancelTasks", Plugin.class);
            cancel.invoke(globalRegionScheduler, plugin);
        } catch (ReflectiveOperationException ignored) {
            // Best-effort; Folia shuts tasks down with the plugin anyway.
        }
    }

    private void invoke(ReflectiveRunnable runnable) {
        try {
            runnable.run();
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Folia scheduler call failed: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ReflectiveRunnable {
        Object run() throws ReflectiveOperationException;
    }

    private static final class ScheduledTaskHandle implements CancellableTask {
        private final Object handle;
        private volatile boolean cancelled;

        ScheduledTaskHandle(Object handle) {
            this.handle = handle;
        }

        @Override
        public void cancel() {
            if (handle == null || cancelled) {
                return;
            }
            try {
                handle.getClass().getMethod("cancel").invoke(handle);
                cancelled = true;
            } catch (ReflectiveOperationException ignored) {
                // Silent fail — task will be reaped on plugin disable.
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
