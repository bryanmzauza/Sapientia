package dev.brmz.sapientia.core.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.core.collect.LongObjectMap;
import dev.brmz.sapientia.core.engine.BlockPositions;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory index of placed Sapientia blocks, so the lifecycle listener can
 * answer {@code "what is at this Block?"} without a SQLite round-trip.
 *
 * <p>Storage is compact: per loaded chunk, a sorted array of ints packing the
 * block's position inside the chunk and a numeric block type, about four bytes
 * per block instead of a key object and a map entry.
 * Chunks are read on the database thread and applied on the main thread by
 * {@link dev.brmz.sapientia.core.engine.ChunkHydrator}. Main thread only.
 * See docs/internal/sapientia-architecture.md §7.4.
 */
public final class ChunkBlockIndex {

    /** Notified when indexed blocks appear or disappear (for example, the machine engine). */
    public interface Observer {
        void onBlockAdded(@NotNull BlockKey key, @NotNull SapientiaBlock definition);

        void onBlockRemoved(@NotNull BlockKey key, @NotNull SapientiaBlock definition);

        /** Every indexed block of the chunk is gone (chunk unloaded). */
        void onChunkUnloaded(@NotNull String world, int chunkX, int chunkZ);
    }

    private final Logger logger;
    private final CustomBlockStore store;
    private final SapientiaBlockRegistry registry;
    private final Map<String, LongObjectMap<ChunkBlocks>> worlds = new HashMap<>();
    private final List<SapientiaBlock> typesById = new ArrayList<>();
    private final Map<SapientiaBlock, Short> idsByType = new IdentityHashMap<>();
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final Map<ChunkKey, Long> pendingLoads = new ConcurrentHashMap<>();
    private final AtomicLong nextToken = new AtomicLong();

    private static final int TYPE_BITS = 12;
    private static final int TYPE_MASK = (1 << TYPE_BITS) - 1;

    public ChunkBlockIndex(
            @NotNull Logger logger,
            @NotNull CustomBlockStore store,
            @NotNull SapientiaBlockRegistry registry) {
        this.logger = logger;
        this.store = store;
        this.registry = registry;
    }

    public void addObserver(@NotNull Observer observer) {
        observers.add(observer);
    }

    // --- Loading ---------------------------------------------------------------------

    /**
     * Starts loading a chunk and returns the token {@link #apply} must present.
     * Loading again or unloading the chunk invalidates earlier tokens.
     */
    public long beginLoad(@NotNull String world, int chunkX, int chunkZ) {
        long token = nextToken.incrementAndGet();
        pendingLoads.put(new ChunkKey(world, chunkX, chunkZ), token);
        return token;
    }

    /**
     * Adds the blocks stored for a chunk. Returns {@code false}, adding nothing,
     * when the chunk was unloaded or loaded again since {@link #beginLoad}.
     * Blocks placed while the read was in flight win over stored ones.
     */
    public boolean apply(@NotNull String world, int chunkX, int chunkZ, long token,
                         @NotNull List<CustomBlockStore.StoredBlock> stored) {
        ChunkKey ck = new ChunkKey(world, chunkX, chunkZ);
        Long expected = pendingLoads.get(ck);
        if (expected == null || expected != token) {
            return false;
        }
        pendingLoads.remove(ck);
        if (stored.isEmpty()) {
            return true;
        }
        ChunkBlocks chunk = chunk(world, chunkX, chunkZ, true);
        for (CustomBlockStore.StoredBlock entry : stored) {
            NamespacedKey id = NamespacedKey.fromString(entry.itemId());
            if (id == null) {
                logger.log(Level.WARNING,
                        "Stored block at " + entry.key() + " has malformed id: " + entry.itemId());
                continue;
            }
            SapientiaBlock def = registry.find(id).orElse(null);
            if (def == null) {
                logger.log(Level.WARNING,
                        "Stored block at " + entry.key() + " references unknown id " + id
                                + "; it will be ignored until the owning content is registered.");
                continue;
            }
            BlockKey key = entry.key();
            int local = local(key.x(), key.y(), key.z());
            if (chunk.get(local) < 0) {
                chunk.put(local, typeId(def));
                for (Observer observer : observers) {
                    observer.onBlockAdded(key, def);
                }
            }
        }
        return true;
    }

    /** Loads a chunk on the calling thread (start-up and tests). */
    public void hydrate(@NotNull String world, int chunkX, int chunkZ) {
        apply(world, chunkX, chunkZ, beginLoad(world, chunkX, chunkZ), store.loadChunk(world, chunkX, chunkZ));
    }

    /** Drops a chunk from the index and cancels a load in flight. */
    public void unloadChunk(@NotNull String world, int chunkX, int chunkZ) {
        pendingLoads.remove(new ChunkKey(world, chunkX, chunkZ));
        LongObjectMap<ChunkBlocks> chunks = worlds.get(world);
        ChunkBlocks removed = chunks == null ? null : chunks.remove(BlockPositions.chunk(chunkX, chunkZ));
        if (removed != null && removed.size() > 0) {
            for (Observer observer : observers) {
                observer.onChunkUnloaded(world, chunkX, chunkZ);
            }
        }
    }

    // --- Queries and edits -----------------------------------------------------------

    /** Returns the Sapientia block at the given world block, or {@code null}. */
    public @Nullable SapientiaBlock at(@NotNull Block block) {
        return at(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public @Nullable SapientiaBlock at(@NotNull BlockKey key) {
        return at(key.world(), key.x(), key.y(), key.z());
    }

    public @Nullable SapientiaBlock at(@NotNull String world, int x, int y, int z) {
        ChunkBlocks chunk = chunk(world, x >> 4, z >> 4, false);
        if (chunk == null) return null;
        short type = chunk.get(local(x, y, z));
        return type < 0 ? null : typesById.get(type);
    }

    /** Records a newly placed block into the in-memory index. */
    public void put(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
        ChunkBlocks chunk = chunk(key.world(), key.chunkX(), key.chunkZ(), true);
        short previous = chunk.put(local(key.x(), key.y(), key.z()), typeId(definition));
        for (Observer observer : observers) {
            if (previous >= 0) {
                observer.onBlockRemoved(key, typesById.get(previous));
            }
            observer.onBlockAdded(key, definition);
        }
    }

    /** Removes a block entry from the in-memory index. */
    public void remove(@NotNull BlockKey key) {
        ChunkBlocks chunk = chunk(key.world(), key.chunkX(), key.chunkZ(), false);
        if (chunk == null) return;
        short removed = chunk.remove(local(key.x(), key.y(), key.z()));
        if (removed >= 0) {
            for (Observer observer : observers) {
                observer.onBlockRemoved(key, typesById.get(removed));
            }
        }
    }

    /** Number of indexed blocks in loaded chunks. */
    public int size() {
        int total = 0;
        for (LongObjectMap<ChunkBlocks> chunks : worlds.values()) {
            int[] sum = {0};
            chunks.forEachValue(c -> sum[0] += c.size());
            total += sum[0];
        }
        return total;
    }

    /** Number of indexed blocks in one chunk. */
    public int chunkSize(@NotNull String world, int chunkX, int chunkZ) {
        ChunkBlocks chunk = chunk(world, chunkX, chunkZ, false);
        return chunk == null ? 0 : chunk.size();
    }

    /** How many blocks of each type one chunk holds. */
    public @NotNull Map<SapientiaBlock, Integer> countsInChunk(@NotNull String world, int chunkX, int chunkZ) {
        Map<SapientiaBlock, Integer> out = new LinkedHashMap<>();
        ChunkBlocks chunk = chunk(world, chunkX, chunkZ, false);
        if (chunk == null) return out;
        int[] counts = new int[typesById.size()];
        chunk.countTypes(counts);
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) out.put(typesById.get(i), counts[i]);
        }
        return out;
    }

    // --- Internals -------------------------------------------------------------------

    private @Nullable ChunkBlocks chunk(String world, int chunkX, int chunkZ, boolean create) {
        LongObjectMap<ChunkBlocks> chunks = worlds.get(world);
        if (chunks == null) {
            if (!create) return null;
            chunks = new LongObjectMap<>();
            worlds.put(world, chunks);
        }
        long key = BlockPositions.chunk(chunkX, chunkZ);
        ChunkBlocks chunk = chunks.get(key);
        if (chunk == null && create) {
            chunk = new ChunkBlocks();
            chunks.put(key, chunk);
        }
        return chunk;
    }

    private short typeId(SapientiaBlock definition) {
        Short id = idsByType.get(definition);
        if (id == null) {
            if (typesById.size() > TYPE_MASK) {
                throw new IllegalStateException("Too many Sapientia block types");
            }
            id = (short) typesById.size();
            typesById.add(definition);
            idsByType.put(definition, id);
        }
        return id;
    }

    /** Position inside the chunk: 4 bits of x, 4 of z, 12 of y (−2048..2047). */
    private static int local(int x, int y, int z) {
        return ((y + 2048) & 0xFFF) << 8 | (x & 15) << 4 | (z & 15);
    }

    private record ChunkKey(String world, int x, int z) {}

    /**
     * One chunk's blocks as a sorted array of ints, each packing the local
     * position (high 20 bits) and the type id (low 12 bits): four bytes per
     * block. Lookups binary-search at most a few thousand entries.
     */
    private static final class ChunkBlocks {
        private int[] entries = new int[4];
        private int size;

        int size() {
            return size;
        }

        short get(int local) {
            int i = find(local);
            return i < 0 ? -1 : (short) (entries[i] & TYPE_MASK);
        }

        short put(int local, short type) {
            int entry = local << TYPE_BITS | type;
            int i = find(local);
            if (i >= 0) {
                short previous = (short) (entries[i] & TYPE_MASK);
                entries[i] = entry;
                return previous;
            }
            i = -(i + 1);
            if (size == entries.length) {
                entries = Arrays.copyOf(entries, size + (size >> 2) + 4);
            }
            System.arraycopy(entries, i, entries, i + 1, size - i);
            entries[i] = entry;
            size++;
            return -1;
        }

        short remove(int local) {
            int i = find(local);
            if (i < 0) return -1;
            short previous = (short) (entries[i] & TYPE_MASK);
            System.arraycopy(entries, i + 1, entries, i, size - i - 1);
            size--;
            return previous;
        }

        void countTypes(int[] counts) {
            for (int i = 0; i < size; i++) {
                counts[entries[i] & TYPE_MASK]++;
            }
        }

        private int find(int local) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int key = entries[mid] >>> TYPE_BITS;
                if (key < local) {
                    lo = mid + 1;
                } else if (key > local) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(lo + 1);
        }
    }
}
