package dev.brmz.sapientia.core.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.android.AndroidNode;
import dev.brmz.sapientia.api.android.AndroidService;
import dev.brmz.sapientia.api.android.AndroidType;
import dev.brmz.sapientia.api.android.AndroidUpgrade;
import dev.brmz.sapientia.api.android.AndroidUpgradeKind;
import dev.brmz.sapientia.api.logic.LogicService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory + SQLite-backed implementation of {@link AndroidService}
 * (T-451 / 1.9.0).
 *
 * <p>Stores androids keyed by world + block coordinates so the cap
 * enforcement (T-456) and the {@link AndroidTicker} can iterate them
 * cheaply. Persistence is synchronous-on-write through {@link AndroidStore};
 * the per-tick fuel / health drift is so small it does not need the
 * write-behind queue used for energy nodes.
 */
public final class AndroidServiceImpl implements AndroidService {

    private final Logger logger;
    private final AndroidStore store;
    private final LogicService logicService;
    private final AndroidConfig config;

    private final ConcurrentMap<BlockKey, SimpleAndroidNode> nodes = new ConcurrentHashMap<>();
    /** Fast O(1) per-chunk count for cap checks (T-456). */
    private final ConcurrentMap<ChunkKey, AtomicInteger> chunkCounts = new ConcurrentHashMap<>();
    private final AtomicInteger total = new AtomicInteger(0);

    public AndroidServiceImpl(@NotNull Logger logger,
                              @NotNull AndroidStore store,
                              @NotNull LogicService logicService,
                              @NotNull AndroidConfig config) {
        this.logger = logger;
        this.store = store;
        this.logicService = logicService;
        this.config = config;
    }

    /**
     * Loads every persisted android into memory. Called from
     * {@code SapientiaPlugin#onEnable} after the database is up.
     */
    public void hydrate() {
        for (AndroidStore.StoredAndroid row : store.loadAll()) {
            World world = Bukkit.getWorld(row.world());
            if (world == null) {
                // World not loaded yet — re-hydrated lazily by the chunk-load hook.
                continue;
            }
            Block block = world.getBlockAt(row.blockX(), row.blockY(), row.blockZ());
            SimpleAndroidNode node = new SimpleAndroidNode(row.type(), block, row.ownerUuid());
            node.setProgramName(row.programName());
            node.setUpgrade(new AndroidUpgrade(AndroidUpgradeKind.AI_CHIP,     clampTier(row.chipTier())));
            node.setUpgrade(new AndroidUpgrade(AndroidUpgradeKind.MOTOR,       clampTier(row.motorTier())));
            node.setUpgrade(new AndroidUpgrade(AndroidUpgradeKind.ARMOUR,      clampTier(row.armourTier())));
            node.setUpgrade(new AndroidUpgrade(AndroidUpgradeKind.FUEL_MODULE, clampTier(row.fuelTier())));
            node.setFuelBuffer(row.fuelBuffer());
            node.setHealth(row.health());
            node.setLastTickMs(row.lastTickMs());
            BlockKey key = BlockKey.from(block);
            if (nodes.putIfAbsent(key, node) == null) {
                bumpCount(key, +1);
            }
        }
        logger.info("[AndroidService] hydrated " + total.get() + " android(s).");
    }

    @Override
    public boolean addNode(@NotNull Block block, @NotNull AndroidType type, UUID ownerUuid) {
        BlockKey key = BlockKey.from(block);
        if (nodes.containsKey(key)) return false;
        ChunkKey chunkKey = key.toChunkKey();
        int currentChunk = chunkCount(chunkKey);
        if (currentChunk >= AndroidCaps.CHUNK_CAP) return false;
        if (total.get() >= config.serverCap()) return false;

        SimpleAndroidNode node = new SimpleAndroidNode(type, block, ownerUuid);
        if (nodes.putIfAbsent(key, node) != null) return false;
        bumpCount(key, +1);
        store.upsert(toStored(node));
        return true;
    }

    @Override
    public void removeNode(@NotNull Block block) {
        BlockKey key = BlockKey.from(block);
        if (nodes.remove(key) != null) {
            bumpCount(key, -1);
            store.delete(key.world(), key.x(), key.y(), key.z());
        }
    }

    @Override
    public @NotNull Optional<AndroidNode> nodeAt(@NotNull Block block) {
        return Optional.ofNullable(nodes.get(BlockKey.from(block)));
    }

    @Override
    public @NotNull Collection<AndroidNode> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(nodes.values()));
    }

    @Override
    public int countInChunk(@NotNull String world, int chunkX, int chunkZ) {
        return chunkCount(new ChunkKey(world, chunkX, chunkZ));
    }

    @Override
    public int totalCount() {
        return total.get();
    }

    @Override
    public boolean assignProgram(@NotNull Block block, @NotNull String programName) {
        SimpleAndroidNode node = nodes.get(BlockKey.from(block));
        if (node == null) return false;
        // Cycle detection / DAG validation lives in LogicService (T-302); we
        // only require the program to already be registered.
        if (logicService.get(programName).isEmpty()) return false;
        node.setProgramName(programName);
        store.upsert(toStored(node));
        return true;
    }

    @Override
    public boolean clearProgram(@NotNull Block block) {
        SimpleAndroidNode node = nodes.get(BlockKey.from(block));
        if (node == null) return false;
        node.setProgramName(null);
        store.upsert(toStored(node));
        return true;
    }

    @Override
    public boolean setUpgrade(@NotNull Block block, @NotNull AndroidUpgrade upgrade) {
        SimpleAndroidNode node = nodes.get(BlockKey.from(block));
        if (node == null) return false;
        node.setUpgrade(upgrade);
        store.upsert(toStored(node));
        return true;
    }

    @Override public int chunkCap()  { return AndroidCaps.CHUNK_CAP; }
    @Override public int serverCap() { return config.serverCap(); }

    // ---- internals ---------------------------------------------------------

    /**
     * Snapshot of every node, used by the ticker. Kept package-private so
     * the implementation can return {@link SimpleAndroidNode}s directly.
     */
    @NotNull List<SimpleAndroidNode> snapshot() {
        return new ArrayList<>(nodes.values());
    }

    void persist(@NotNull SimpleAndroidNode node) {
        store.upsert(toStored(node));
    }

    private int chunkCount(@NotNull ChunkKey key) {
        AtomicInteger c = chunkCounts.get(key);
        return c == null ? 0 : c.get();
    }

    private void bumpCount(@NotNull BlockKey key, int delta) {
        ChunkKey chunkKey = key.toChunkKey();
        chunkCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0)).addAndGet(delta);
        total.addAndGet(delta);
        if (delta < 0) {
            chunkCounts.computeIfPresent(chunkKey, (k, v) -> v.get() <= 0 ? null : v);
        }
    }

    private static int clampTier(int tier) {
        if (tier < 1) return 1;
        if (tier > 4) return 4;
        return tier;
    }

    private AndroidStore.StoredAndroid toStored(@NotNull SimpleAndroidNode node) {
        Block b = node.block();
        @Nullable UUID owner = node.ownerUuid().orElse(null);
        @Nullable String program = node.programName().orElse(null);
        return new AndroidStore.StoredAndroid(
                b.getWorld().getName(), b.getX(), b.getY(), b.getZ(),
                node.type(), owner, program,
                node.upgradeTier(AndroidUpgradeKind.AI_CHIP),
                node.upgradeTier(AndroidUpgradeKind.MOTOR),
                node.upgradeTier(AndroidUpgradeKind.ARMOUR),
                node.upgradeTier(AndroidUpgradeKind.FUEL_MODULE),
                node.fuelBuffer(), node.health(), node.lastTickMs());
    }

    private record BlockKey(@NotNull String world, int x, int y, int z) {
        static @NotNull BlockKey from(@NotNull Block b) {
            return new BlockKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        }
        @NotNull ChunkKey toChunkKey() {
            return new ChunkKey(world, x >> 4, z >> 4);
        }
    }

    private record ChunkKey(@NotNull String world, int chunkX, int chunkZ) {}
}
