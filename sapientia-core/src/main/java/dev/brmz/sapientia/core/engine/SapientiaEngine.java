package dev.brmz.sapientia.core.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.collect.IntList;
import dev.brmz.sapientia.core.collect.LongObjectMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single tick loop of Sapientia. Every tick it:
 * <ol>
 *   <li>runs the periodic system tasks (network solvers, logic, androids);</li>
 *   <li>runs the machines due this tick through the {@link MachineScheduler},
 *       within the configured time budget;</li>
 *   <li>records the cost of each part in the {@link PerfMonitor}.</li>
 * </ol>
 *
 * <p>Machines are blocks whose type has a registered {@link MachineBehavior}.
 * They join the scheduler when their chunk is indexed and leave when it
 * unloads, and they pause while no player is within the activity radius.
 */
public final class SapientiaEngine implements ChunkBlockIndex.Observer, ActivityMap.Listener, ActivityFilter {

    private record SystemTask(String name, int period, int offset, Runnable task) {}

    private static final int NO_HANDLE = -1;

    private final Logger logger;
    private final EngineConfig config;
    private final MachineScheduler scheduler;
    private final ActivityMap activity;
    private final PerfMonitor perf = new PerfMonitor();
    private final List<SystemTask> systemTasks = new ArrayList<>();
    private final List<ActivityMap.Listener> activityListeners = new ArrayList<>();

    private final Map<NamespacedKey, Integer> behaviorIds = new HashMap<>();
    private final Map<NamespacedKey, Integer> initialDelays = new HashMap<>();
    private final List<NamespacedKey> behaviorBlocks = new ArrayList<>();
    private volatile boolean[] lockedBehaviors = new boolean[0];
    // Per world id: machine handles by packed chunk. A chunk holds at most a few hundred
    // processors, so finding one machine scans its chunk instead of keeping a per-block map.
    private final List<LongObjectMap<IntList>> machinesByWorld = new ArrayList<>();

    private final List<String> worldNames = new ArrayList<>();
    private final Map<String, Integer> worldIds = new HashMap<>();
    private final List<World> worldCache = new ArrayList<>();

    private long tick;

    public SapientiaEngine(@NotNull Logger logger, @NotNull EngineConfig config) {
        this.logger = logger;
        this.config = config;
        this.scheduler = new MachineScheduler(logger);
        this.activity = new ActivityMap(config.activityRadius(), config.deactivateDelayTicks(), this);
    }

    public @NotNull EngineConfig config() { return config; }

    public @NotNull ActivityMap activity() { return activity; }

    public @NotNull PerfMonitor perf() { return perf; }

    public @NotNull MachineScheduler scheduler() { return scheduler; }

    public long currentTick() { return tick; }

    // --- Registration ------------------------------------------------------------

    /**
     * Makes every block with this id a machine run by {@code behavior}. The
     * first run happens after a random delay up to {@code firstRunSpread} ticks,
     * so machines loaded together do not all run on the same tick.
     */
    public void registerBehavior(@NotNull NamespacedKey blockId, int firstRunSpread,
                                 @NotNull MachineBehavior behavior) {
        if (behaviorIds.containsKey(blockId)) {
            throw new IllegalStateException("Behaviour already registered for " + blockId);
        }
        int index = behaviorBlocks.size();
        behaviorBlocks.add(blockId);
        behaviorIds.put(blockId, scheduler.addBehavior(context -> {
            boolean[] locked = lockedBehaviors;
            return index < locked.length && locked[index]
                    ? MachineBehavior.idle(MachineScheduler.MAX_IDLE_DELAY)
                    : behavior.run(context);
        }));
        initialDelays.put(blockId, Math.max(1, firstRunSpread));
    }

    /**
     * Recomputes which machine types are locked (their era is not unlocked).
     * Locked machines stay loaded but only check back every 10 seconds.
     */
    public void refreshLocks(@NotNull java.util.function.Predicate<NamespacedKey> isLocked) {
        boolean[] locked = new boolean[behaviorBlocks.size()];
        for (int i = 0; i < locked.length; i++) {
            locked[i] = isLocked.test(behaviorBlocks.get(i));
        }
        lockedBehaviors = locked;
    }

    /** Whether blocks of this id run on the machine scheduler (they count as processors). */
    public boolean isProcessor(@NotNull NamespacedKey blockId) {
        return behaviorIds.containsKey(blockId);
    }

    /** Adds a task run every {@code period} ticks, starting at {@code offset}. */
    public void addSystemTask(@NotNull String name, int period, int offset, @NotNull Runnable task) {
        systemTasks.add(new SystemTask(name, Math.max(1, period), Math.max(0, offset), task));
    }

    /** Also notifies {@code listener} when chunks enter or leave the activity radius. */
    public void addActivityListener(@NotNull ActivityMap.Listener listener) {
        activityListeners.add(listener);
    }

    /** Wakes the machine at this block, if any (something it waits for changed). */
    public void wake(@NotNull BlockKey key) {
        int handle = findHandle(key);
        if (handle != NO_HANDLE) {
            scheduler.wake(handle);
        }
    }

    // --- Tick ----------------------------------------------------------------------

    /** Runs one server tick. Called by the plugin's single repeating task. */
    public void tick() {
        long now = ++tick;
        if (now % 20 == 0) {
            activity.sweep(now);
        }
        for (SystemTask task : systemTasks) {
            if (now >= task.offset() && (now - task.offset()) % task.period() == 0) {
                long start = System.nanoTime();
                try {
                    task.task().run();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "System task " + task.name() + " failed", e);
                }
                perf.record(task.name(), System.nanoTime() - start);
            }
        }
        long start = System.nanoTime();
        scheduler.tick(now, config.tickBudgetNanos());
        perf.record("machines", System.nanoTime() - start);
        perf.endTick();
    }

    // --- Machines and chunks ---------------------------------------------------------

    @Override
    public void onBlockAdded(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
        Integer behavior = behaviorIds.get(definition.id());
        if (behavior == null || findHandle(key) != NO_HANDLE) {
            return;
        }
        int worldId = worldId(key.world());
        long position = BlockPositions.pack(key.x(), key.y(), key.z());
        int spread = initialDelays.getOrDefault(definition.id(), 20);
        int handle = scheduler.register(worldId, position, behavior, 1 + ThreadLocalRandom.current().nextInt(spread));
        if (!isActive(key)) {
            scheduler.pause(handle);
        }
        long chunk = BlockPositions.chunk(key.chunkX(), key.chunkZ());
        LongObjectMap<IntList> machines = machinesByWorld.get(worldId);
        IntList list = machines.get(chunk);
        if (list == null) {
            list = new IntList();
            machines.put(chunk, list);
        }
        list.add(handle);
    }

    @Override
    public void onBlockRemoved(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
        int handle = findHandle(key);
        if (handle == NO_HANDLE) {
            return;
        }
        scheduler.unregister(handle);
        long chunk = BlockPositions.chunk(key.chunkX(), key.chunkZ());
        LongObjectMap<IntList> machines = machinesByWorld.get(worldId(key.world()));
        IntList list = machines.get(chunk);
        list.removeValue(handle);
        if (list.isEmpty()) {
            machines.remove(chunk);
        }
    }

    @Override
    public void onChunkUnloaded(@NotNull String world, int chunkX, int chunkZ) {
        IntList list = machinesByWorld.get(worldId(world)).remove(BlockPositions.chunk(chunkX, chunkZ));
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            scheduler.unregister(list.get(i));
        }
    }

    private int findHandle(BlockKey key) {
        IntList list = machinesByWorld.get(worldId(key.world())).get(BlockPositions.chunk(key.chunkX(), key.chunkZ()));
        if (list == null) {
            return NO_HANDLE;
        }
        long position = BlockPositions.pack(key.x(), key.y(), key.z());
        for (int i = 0; i < list.size(); i++) {
            int handle = list.get(i);
            if (scheduler.position(handle) == position) {
                return handle;
            }
        }
        return NO_HANDLE;
    }

    @Override
    public void onChunkActivated(@NotNull String world, int chunkX, int chunkZ) {
        for (ActivityMap.Listener listener : activityListeners) {
            listener.onChunkActivated(world, chunkX, chunkZ);
        }
        IntList list = machinesByWorld.get(worldId(world)).get(BlockPositions.chunk(chunkX, chunkZ));
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            scheduler.resume(list.get(i), 1 + ThreadLocalRandom.current().nextInt(20));
        }
    }

    @Override
    public void onChunkDeactivated(@NotNull String world, int chunkX, int chunkZ) {
        for (ActivityMap.Listener listener : activityListeners) {
            listener.onChunkDeactivated(world, chunkX, chunkZ);
        }
        IntList list = machinesByWorld.get(worldId(world)).get(BlockPositions.chunk(chunkX, chunkZ));
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            scheduler.pause(list.get(i));
        }
    }

    @Override
    public boolean isActive(@NotNull String world, int chunkX, int chunkZ) {
        return activity.isActive(world, chunkX, chunkZ);
    }

    // --- Helpers for behaviours ---------------------------------------------------------

    /** Location of the machine being run. */
    public @NotNull BlockKey keyOf(@NotNull MachineContext context) {
        return new BlockKey(worldNames.get(context.worldId()), context.x(), context.y(), context.z());
    }

    /** Block of the machine being run, or {@code null} if its world is gone. */
    public @Nullable Block blockOf(@NotNull MachineContext context) {
        World world = world(context.worldId());
        return world == null ? null : world.getBlockAt(context.x(), context.y(), context.z());
    }

    private @Nullable World world(int worldId) {
        World cached = worldCache.get(worldId);
        if (cached == null) {
            cached = Bukkit.getWorld(worldNames.get(worldId));
            worldCache.set(worldId, cached);
        }
        return cached;
    }

    /** Forgets cached world references (world unloaded or reloaded). */
    public void clearWorldCache() {
        for (int i = 0; i < worldCache.size(); i++) {
            worldCache.set(i, null);
        }
    }

    private int worldId(String name) {
        Integer id = worldIds.get(name);
        if (id == null) {
            id = worldNames.size();
            worldNames.add(name);
            worldCache.add(null);
            machinesByWorld.add(new LongObjectMap<>());
            worldIds.put(name, id);
        }
        return id;
    }

    /** Number of machines per state, for {@code /sapientia perf}. */
    public @NotNull Map<String, Integer> machineCounts() {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        out.put("registered", scheduler.registeredCount());
        out.put("scheduled", scheduler.countInState(MachineScheduler.SCHEDULED));
        out.put("sleeping", scheduler.countInState(MachineScheduler.SLEEPING));
        out.put("paused", scheduler.countInState(MachineScheduler.PAUSED));
        out.put("backlog", scheduler.readyBacklog());
        return out;
    }
}
