package dev.brmz.sapientia.core.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps an in-memory index of placed Sapientia blocks keyed by chunk so the
 * lifecycle listener can answer {@code "what is at this Block?"} in O(1) without
 * a SQLite round-trip. Hydrated on {@link ChunkLoadEvent}; dropped on
 * {@link ChunkUnloadEvent}. See docs/sapientia-architecture.md §7.4.
 */
public final class ChunkBlockIndex implements Listener {

    private final Logger logger;
    private final CustomBlockStore store;
    private final SapientiaBlockRegistry registry;
    private final Map<ChunkKey, Map<BlockKey, SapientiaBlock>> loadedChunks = new ConcurrentHashMap<>();

    public ChunkBlockIndex(
            @NotNull Logger logger,
            @NotNull CustomBlockStore store,
            @NotNull SapientiaBlockRegistry registry) {
        this.logger = logger;
        this.store = store;
        this.registry = registry;
    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        hydrate(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        loadedChunks.remove(new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    /** Hydrates a single chunk into the in-memory index. Safe to call repeatedly. */
    public void hydrate(@NotNull String world, int chunkX, int chunkZ) {
        ChunkKey ck = new ChunkKey(world, chunkX, chunkZ);
        Map<BlockKey, SapientiaBlock> map = new HashMap<>();
        for (CustomBlockStore.StoredBlock stored : store.loadChunk(world, chunkX, chunkZ)) {
            NamespacedKey id = NamespacedKey.fromString(stored.itemId());
            if (id == null) {
                logger.log(Level.WARNING,
                        "Stored block at " + stored.key() + " has malformed id: " + stored.itemId());
                continue;
            }
            SapientiaBlock def = registry.find(id).orElse(null);
            if (def == null) {
                logger.log(Level.WARNING,
                        "Stored block at " + stored.key() + " references unknown id " + id
                                + "; it will be ignored until the owning content is registered.");
                continue;
            }
            map.put(stored.key(), def);
        }
        loadedChunks.put(ck, map);
    }

    /** Returns the Sapientia block at the given world block, or {@code null}. */
    public @Nullable SapientiaBlock at(@NotNull Block block) {
        return at(new BlockKey(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ()));
    }

    public @Nullable SapientiaBlock at(@NotNull BlockKey key) {
        Map<BlockKey, SapientiaBlock> chunk = loadedChunks.get(
                new ChunkKey(key.world(), key.chunkX(), key.chunkZ()));
        return chunk == null ? null : chunk.get(key);
    }

    /** Records a newly placed block into the in-memory index. */
    public void put(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
        loadedChunks
                .computeIfAbsent(new ChunkKey(key.world(), key.chunkX(), key.chunkZ()),
                        k -> new HashMap<>())
                .put(key, definition);
    }

    /** Removes a block entry from the in-memory index. */
    public void remove(@NotNull BlockKey key) {
        Map<BlockKey, SapientiaBlock> chunk = loadedChunks.get(
                new ChunkKey(key.world(), key.chunkX(), key.chunkZ()));
        if (chunk != null) {
            chunk.remove(key);
        }
    }

    public int size() {
        return loadedChunks.values().stream().mapToInt(Map::size).sum();
    }

    public @NotNull Map<BlockKey, SapientiaBlock> viewChunk(
            @NotNull String world, int chunkX, int chunkZ) {
        Map<BlockKey, SapientiaBlock> m = loadedChunks.get(new ChunkKey(world, chunkX, chunkZ));
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
    }

    private record ChunkKey(String world, int x, int z) {}
}
