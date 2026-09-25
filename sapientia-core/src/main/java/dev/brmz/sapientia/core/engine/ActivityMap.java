package dev.brmz.sapientia.core.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Tracks which chunks are close enough to a player for machines to run.
 *
 * <p>A chunk is active while at least one player is within {@code radius}
 * chunks of it (a square of {@code 2 * radius + 1} chunks per player). When
 * the last player leaves, the chunk stays active for {@code delayTicks} more,
 * so walking along the edge of the radius does not toggle machines on and off.
 *
 * <p>Updates happen only when a player crosses a chunk border, joins, leaves
 * or changes world, never per tick. Pure Java; the Bukkit wiring lives in
 * {@link ActivityTracker}.
 */
public final class ActivityMap {

    /** Receives activation changes. */
    public interface Listener {
        void onChunkActivated(@NotNull String world, int chunkX, int chunkZ);

        void onChunkDeactivated(@NotNull String world, int chunkX, int chunkZ);
    }

    private final int radius;
    private final long delayTicks;
    private final Listener listener;
    private final Map<String, Map<Long, Integer>> watchers = new HashMap<>();
    private final Map<String, Map<Long, Long>> pendingDeactivation = new HashMap<>();

    public ActivityMap(int radius, long delayTicks, @NotNull Listener listener) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be >= 0");
        }
        this.radius = radius;
        this.delayTicks = Math.max(0, delayTicks);
        this.listener = listener;
    }

    public int radius() {
        return radius;
    }

    public boolean isActive(@NotNull String world, int chunkX, int chunkZ) {
        long key = BlockPositions.chunk(chunkX, chunkZ);
        Map<Long, Integer> counts = watchers.get(world);
        if (counts != null && counts.containsKey(key)) {
            return true;
        }
        Map<Long, Long> pending = pendingDeactivation.get(world);
        return pending != null && pending.containsKey(key);
    }

    /** A player appeared at the given chunk (join, respawn, world change). */
    public void addViewer(@NotNull String world, int chunkX, int chunkZ, long now) {
        forEachInSquare(chunkX, chunkZ, (x, z) -> increment(world, x, z));
    }

    /** A player left the given chunk (quit, world change). */
    public void removeViewer(@NotNull String world, int chunkX, int chunkZ, long now) {
        forEachInSquare(chunkX, chunkZ, (x, z) -> decrement(world, x, z, now));
    }

    /** A player moved between chunks; only the difference between both squares changes. */
    public void moveViewer(@NotNull String fromWorld, int fromX, int fromZ,
                           @NotNull String toWorld, int toX, int toZ, long now) {
        if (!fromWorld.equals(toWorld)) {
            addViewer(toWorld, toX, toZ, now);
            removeViewer(fromWorld, fromX, fromZ, now);
            return;
        }
        if (fromX == toX && fromZ == toZ) {
            return;
        }
        // Add first so chunks covered by both squares never drop to zero.
        forEachInSquare(toX, toZ, (x, z) -> {
            if (!inSquare(fromX, fromZ, x, z)) increment(toWorld, x, z);
        });
        forEachInSquare(fromX, fromZ, (x, z) -> {
            if (!inSquare(toX, toZ, x, z)) decrement(fromWorld, x, z, now);
        });
    }

    /** Deactivates chunks whose delay has passed. Call periodically (for example every second). */
    public void sweep(long now) {
        for (Map.Entry<String, Map<Long, Long>> byWorld : pendingDeactivation.entrySet()) {
            Iterator<Map.Entry<Long, Long>> it = byWorld.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Long> entry = it.next();
                if (entry.getValue() <= now) {
                    it.remove();
                    long key = entry.getKey();
                    listener.onChunkDeactivated(byWorld.getKey(),
                            BlockPositions.chunkX(key), BlockPositions.chunkZ(key));
                }
            }
        }
    }

    /** Number of chunks currently active (watched or waiting to deactivate). */
    public int activeCount() {
        int n = 0;
        for (Map<Long, Integer> m : watchers.values()) n += m.size();
        for (Map<Long, Long> m : pendingDeactivation.values()) n += m.size();
        return n;
    }

    private void increment(String world, int x, int z) {
        long key = BlockPositions.chunk(x, z);
        Integer before = watchers.computeIfAbsent(world, w -> new HashMap<>()).merge(key, 1, Integer::sum) - 1;
        if (before == 0) {
            Map<Long, Long> pending = pendingDeactivation.get(world);
            boolean wasPending = pending != null && pending.remove(key) != null;
            if (!wasPending) {
                listener.onChunkActivated(world, x, z);
            }
        }
    }

    private void decrement(String world, int x, int z, long now) {
        long key = BlockPositions.chunk(x, z);
        Map<Long, Integer> counts = watchers.get(world);
        if (counts == null) {
            return;
        }
        Integer current = counts.get(key);
        if (current == null) {
            return;
        }
        if (current <= 1) {
            counts.remove(key);
            if (delayTicks == 0) {
                listener.onChunkDeactivated(world, x, z);
            } else {
                pendingDeactivation.computeIfAbsent(world, w -> new HashMap<>()).put(key, now + delayTicks);
            }
        } else {
            counts.put(key, current - 1);
        }
    }

    private boolean inSquare(int centerX, int centerZ, int x, int z) {
        return Math.abs(x - centerX) <= radius && Math.abs(z - centerZ) <= radius;
    }

    private void forEachInSquare(int centerX, int centerZ, ChunkVisitor visitor) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                visitor.visit(centerX + dx, centerZ + dz);
            }
        }
    }

    @FunctionalInterface
    private interface ChunkVisitor {
        void visit(int chunkX, int chunkZ);
    }
}
