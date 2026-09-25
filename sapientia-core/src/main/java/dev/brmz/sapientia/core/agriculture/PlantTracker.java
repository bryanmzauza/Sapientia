package dev.brmz.sapientia.core.agriculture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.brmz.sapientia.core.collect.LongObjectMap;
import dev.brmz.sapientia.core.engine.BlockPositions;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Which vanilla crop blocks are Sapientia plants. Stored in each chunk's
 * persistent data as {@code position=plant} strings, only for chunks with
 * Sapientia plants; read on first use and kept in memory while loaded.
 * Growing plants cost nothing: vanilla grows them, the plugin only looks here
 * when a crop is harvested.
 */
public final class PlantTracker {

    private final NamespacedKey key;
    private final Map<UUID, LongObjectMap<Map<Integer, NamespacedKey>>> worlds = new HashMap<>();

    public PlantTracker(@NotNull String namespace) {
        this.key = new NamespacedKey(namespace, "plants");
    }

    public @Nullable NamespacedKey plantAt(@NotNull Block block) {
        return plants(block.getChunk()).get(local(block));
    }

    public void set(@NotNull Block block, @NotNull NamespacedKey plant) {
        Map<Integer, NamespacedKey> plants = plants(block.getChunk());
        if (plant.equals(plants.put(local(block), plant))) return;
        save(block.getChunk(), plants);
    }

    public void clear(@NotNull Block block) {
        Map<Integer, NamespacedKey> plants = plants(block.getChunk());
        if (plants.remove(local(block)) != null) save(block.getChunk(), plants);
    }

    public void onChunkUnloaded(@NotNull Chunk chunk) {
        LongObjectMap<Map<Integer, NamespacedKey>> chunks = worlds.get(chunk.getWorld().getUID());
        if (chunks != null) chunks.remove(BlockPositions.chunk(chunk.getX(), chunk.getZ()));
    }

    public void onWorldUnloaded(@NotNull World world) {
        worlds.remove(world.getUID());
    }

    private Map<Integer, NamespacedKey> plants(Chunk chunk) {
        LongObjectMap<Map<Integer, NamespacedKey>> chunks =
                worlds.computeIfAbsent(chunk.getWorld().getUID(), k -> new LongObjectMap<>());
        long chunkKey = BlockPositions.chunk(chunk.getX(), chunk.getZ());
        Map<Integer, NamespacedKey> plants = chunks.get(chunkKey);
        if (plants == null) {
            plants = new HashMap<>();
            List<String> stored = chunk.getPersistentDataContainer().get(key, PersistentDataType.LIST.strings());
            if (stored != null) {
                for (String entry : stored) {
                    int eq = entry.indexOf('=');
                    if (eq <= 0) continue;
                    NamespacedKey plant = NamespacedKey.fromString(entry.substring(eq + 1));
                    try {
                        if (plant != null) plants.put(Integer.parseInt(entry.substring(0, eq)), plant);
                    } catch (NumberFormatException ignored) {
                        // skip malformed entries
                    }
                }
            }
            chunks.put(chunkKey, plants);
        }
        return plants;
    }

    private void save(Chunk chunk, Map<Integer, NamespacedKey> plants) {
        if (plants.isEmpty()) {
            chunk.getPersistentDataContainer().remove(key);
            return;
        }
        List<String> entries = new ArrayList<>(plants.size());
        for (Map.Entry<Integer, NamespacedKey> e : plants.entrySet()) {
            entries.add(e.getKey() + "=" + e.getValue());
        }
        chunk.getPersistentDataContainer().set(key, PersistentDataType.LIST.strings(), entries);
    }

    /** Position inside the chunk: 4 bits of x, 4 of z, 12 of y (−2048..2047). */
    private static int local(Block block) {
        return ((block.getY() + 2048) & 0xFFF) << 8 | (block.getX() & 15) << 4 | (block.getZ() & 15);
    }
}
