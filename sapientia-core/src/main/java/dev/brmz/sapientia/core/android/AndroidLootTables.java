package dev.brmz.sapientia.core.android;

import java.util.List;
import java.util.Map;
import java.util.Random;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Deterministic loot tables for the kinetic loop (T-455 / 1.9.1).
 *
 * <p>Same approach as the existing {@code mob_simulator} (1.5.0) and
 * the per-block drop list in {@code BlockLifecycleListener}: each android
 * archetype owns a list of weighted entries, and {@link #roll} picks one
 * deterministically from a {@code (type, seed)} pair. Builder and trader
 * have empty tables — they consume their input chest instead of generating
 * loot, so {@link #roll} returns {@code null} for those types.
 *
 * <p>The seed contract (deterministic + reproducible per android per tick)
 * makes the engine testable without Bukkit and makes simulated rewards
 * reproducible for ADR-021 (slayer melee policy).
 */
public final class AndroidLootTables {

    private AndroidLootTables() {}

    /**
     * Pure-POJO drop description. The behaviour engine converts these to
     * {@code ItemStack}s at runtime; keeping the loot table itself
     * Bukkit-free lets the JMH benchmark + unit tests run in plain JVM
     * contexts without a {@code ServerMock}.
     */
    public record LootDrop(@NotNull Material material, int amount) {
        public LootDrop {
            if (amount < 1) throw new IllegalArgumentException("amount must be >= 1");
        }
    }

    private record Entry(@NotNull Material material, int weight, int min, int max) {
        Entry {
            if (weight <= 0) throw new IllegalArgumentException("weight must be > 0");
            if (min < 1 || max < min) throw new IllegalArgumentException("invalid range " + min + ".." + max);
        }
    }

    private static final Map<AndroidType, List<Entry>> TABLES = Map.of(
            AndroidType.FARMER, List.of(
                    new Entry(Material.WHEAT,        4, 1, 3),
                    new Entry(Material.CARROT,       3, 1, 2),
                    new Entry(Material.POTATO,       3, 1, 2),
                    new Entry(Material.BEETROOT,     2, 1, 2),
                    new Entry(Material.PUMPKIN,      1, 1, 1),
                    new Entry(Material.MELON_SLICE,  2, 1, 4)),
            AndroidType.LUMBERJACK, List.of(
                    new Entry(Material.OAK_LOG,      4, 1, 2),
                    new Entry(Material.BIRCH_LOG,    3, 1, 2),
                    new Entry(Material.SPRUCE_LOG,   3, 1, 2),
                    new Entry(Material.JUNGLE_LOG,   2, 1, 1),
                    new Entry(Material.DARK_OAK_LOG, 2, 1, 1),
                    new Entry(Material.ACACIA_LOG,   2, 1, 1)),
            AndroidType.MINER, List.of(
                    new Entry(Material.COAL,         5, 1, 3),
                    new Entry(Material.RAW_IRON,     4, 1, 2),
                    new Entry(Material.RAW_COPPER,   4, 1, 2),
                    new Entry(Material.RAW_GOLD,     2, 1, 1),
                    new Entry(Material.REDSTONE,     3, 1, 4),
                    new Entry(Material.LAPIS_LAZULI, 2, 1, 4),
                    new Entry(Material.DIAMOND,      1, 1, 1),
                    new Entry(Material.EMERALD,      1, 1, 1)),
            AndroidType.FISHERMAN, List.of(
                    new Entry(Material.COD,           5, 1, 2),
                    new Entry(Material.SALMON,        4, 1, 2),
                    new Entry(Material.TROPICAL_FISH, 2, 1, 1),
                    new Entry(Material.PUFFERFISH,    1, 1, 1),
                    new Entry(Material.STRING,        2, 1, 1),
                    new Entry(Material.BONE,          1, 1, 1)),
            AndroidType.BUTCHER, List.of(
                    new Entry(Material.BEEF,          3, 1, 2),
                    new Entry(Material.PORKCHOP,      3, 1, 2),
                    new Entry(Material.CHICKEN,       3, 1, 2),
                    new Entry(Material.MUTTON,        2, 1, 2),
                    new Entry(Material.RABBIT,        1, 1, 1),
                    new Entry(Material.LEATHER,       2, 1, 1),
                    new Entry(Material.FEATHER,       2, 1, 2),
                    new Entry(Material.EGG,           2, 1, 1)),
            AndroidType.SLAYER, List.of(
                    new Entry(Material.ROTTEN_FLESH,  4, 1, 2),
                    new Entry(Material.BONE,          4, 1, 2),
                    new Entry(Material.STRING,        3, 1, 2),
                    new Entry(Material.ARROW,         3, 1, 4),
                    new Entry(Material.GUNPOWDER,     2, 1, 1),
                    new Entry(Material.SPIDER_EYE,    2, 1, 1),
                    new Entry(Material.ENDER_PEARL,   1, 1, 1)));

    /**
     * Picks one drop from the loot table for the given android type. Returns
     * {@code null} when the type has no loot table (builder, trader) — those
     * archetypes use {@link AndroidBehaviorEngine}'s consume / exchange path
     * instead.
     */
    public static @Nullable LootDrop roll(@NotNull AndroidType type, long seed) {
        List<Entry> table = TABLES.get(type);
        if (table == null || table.isEmpty()) return null;

        // Deterministic xorshift-style PRNG via java.util.Random — gives a
        // reproducible roll for any (type, seed) pair which keeps tests +
        // benchmarks honest.
        Random r = new Random(seed);
        int totalWeight = 0;
        for (Entry e : table) totalWeight += e.weight;
        int pick = r.nextInt(totalWeight);
        Entry chosen = table.get(table.size() - 1);
        int acc = 0;
        for (Entry e : table) {
            acc += e.weight;
            if (pick < acc) {
                chosen = e;
                break;
            }
        }
        int amount = chosen.min + r.nextInt(chosen.max - chosen.min + 1);
        return new LootDrop(chosen.material, amount);
    }

    /** Whether the given archetype has a loot table at all. Used by tests + diagnostics. */
    public static boolean hasTable(@NotNull AndroidType type) {
        return TABLES.containsKey(type);
    }

    /** Number of distinct archetypes covered by a loot table. Always 6 (8 minus builder + trader). */
    public static int tableCount() { return TABLES.size(); }
}
