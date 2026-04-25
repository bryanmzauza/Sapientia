package dev.brmz.sapientia.core.fluids;

import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers to read fluid from / write fluid to vanilla blocks adjacent to a
 * Sapientia fluid node (T-301 / 1.2.0). The 1.2.0 integration covers:
 *
 * <ul>
 *   <li>Water / lava cauldrons (one full level == 1000 mB).</li>
 *   <li>Source water / lava blocks (consumed in full when extracted).</li>
 *   <li>Replaceable air blocks for {@code DRAIN} placement.</li>
 * </ul>
 *
 * <p>Powder snow and custom modded fluids are out of scope until 1.3.0+.
 */
final class AdjacentFluids {

    private AdjacentFluids() {}

    static final long BUCKET_MB = 1000L;
    static final long CAULDRON_LEVEL_MB = 1000L; // 3 levels = 3000 mB; matches one bucket per level.

    private static final int[][] OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    /** Result of a successful extract. */
    record Extract(@NotNull FluidType type, long amountMb, @NotNull Block source) {}

    /**
     * Scans 6 neighbours of {@code origin} for a vanilla source matching
     * {@code preferred} (or any source when {@code preferred} is null). Returns
     * the first hit without mutating the world.
     */
    static @Nullable Extract peekSource(@NotNull Block origin,
                                        @Nullable FluidType preferred,
                                        @NotNull FluidType waterType,
                                        @NotNull FluidType lavaType) {
        for (int[] off : OFFSETS) {
            Block b = origin.getRelative(off[0], off[1], off[2]);
            Material mat = b.getType();
            if (mat == Material.WATER_CAULDRON || mat == Material.LAVA_CAULDRON) {
                BlockData data = b.getBlockData();
                if (!(data instanceof Levelled levelled)) continue;
                int level = levelled.getLevel();
                if (level <= 0) continue;
                FluidType cauldronType = mat == Material.WATER_CAULDRON ? waterType : lavaType;
                if (preferred != null && !preferred.id().equals(cauldronType.id())) continue;
                return new Extract(cauldronType, level * CAULDRON_LEVEL_MB, b);
            }
            if (mat == Material.WATER || mat == Material.LAVA) {
                BlockData data = b.getBlockData();
                if (data instanceof Levelled levelled && levelled.getLevel() == 0) {
                    FluidType src = mat == Material.WATER ? waterType : lavaType;
                    if (preferred != null && !preferred.id().equals(src.id())) continue;
                    return new Extract(src, BUCKET_MB, b);
                }
            }
        }
        return null;
    }

    /**
     * Removes {@code amount} mB from {@code source} (mutates the world). Caller is
     * responsible for ensuring {@code amount} does not exceed what
     * {@link #peekSource} reported.
     */
    static long consumeFromSource(@NotNull Block source, long amountMb) {
        Material mat = source.getType();
        if (mat == Material.WATER_CAULDRON || mat == Material.LAVA_CAULDRON) {
            BlockData data = source.getBlockData();
            if (!(data instanceof Levelled levelled)) return 0L;
            int current = levelled.getLevel();
            int levels = (int) Math.min(current, Math.max(1, amountMb / CAULDRON_LEVEL_MB));
            int newLevel = current - levels;
            if (newLevel <= 0) {
                source.setType(Material.CAULDRON, false);
            } else {
                levelled.setLevel(newLevel);
                source.setBlockData(levelled, false);
            }
            return levels * CAULDRON_LEVEL_MB;
        }
        if (mat == Material.WATER || mat == Material.LAVA) {
            source.setType(Material.AIR, false);
            return BUCKET_MB;
        }
        return 0L;
    }

    /**
     * Tries to deposit up to {@code stack.amountMb} into a 6-neighbour of
     * {@code origin}: first an empty/matching cauldron (preferred), then a
     * replaceable air block (places a single source block per bucket). Returns
     * the amount actually placed.
     */
    static long deposit(@NotNull Block origin, @NotNull FluidStack stack,
                        @NotNull FluidType waterType, @NotNull FluidType lavaType) {
        long remaining = stack.amountMb();
        if (remaining <= 0L) return 0L;
        Material targetSource;
        Material targetCauldron;
        if (stack.type().id().equals(waterType.id())) {
            targetSource = Material.WATER;
            targetCauldron = Material.WATER_CAULDRON;
        } else if (stack.type().id().equals(lavaType.id())) {
            targetSource = Material.LAVA;
            targetCauldron = Material.LAVA_CAULDRON;
        } else {
            // Custom fluids: 1.2.0 supports water/lava only for actual block placement.
            return 0L;
        }
        // Pass 1: top up matching cauldrons.
        for (int[] off : OFFSETS) {
            if (remaining < CAULDRON_LEVEL_MB) break;
            Block b = origin.getRelative(off[0], off[1], off[2]);
            if (b.getType() == targetCauldron) {
                BlockData data = b.getBlockData();
                if (!(data instanceof Levelled levelled)) continue;
                int free = levelled.getMaximumLevel() - levelled.getLevel();
                if (free <= 0) continue;
                int addLevels = (int) Math.min(free, remaining / CAULDRON_LEVEL_MB);
                if (addLevels <= 0) continue;
                levelled.setLevel(levelled.getLevel() + addLevels);
                b.setBlockData(levelled, false);
                remaining -= addLevels * CAULDRON_LEVEL_MB;
            } else if (b.getType() == Material.CAULDRON) {
                int addLevels = (int) Math.min(3, remaining / CAULDRON_LEVEL_MB);
                if (addLevels <= 0) continue;
                b.setType(targetCauldron, false);
                BlockData fresh = b.getBlockData();
                if (fresh instanceof Levelled lv) {
                    int max = Math.min(addLevels, lv.getMaximumLevel());
                    lv.setLevel(max);
                    b.setBlockData(lv, false);
                    remaining -= max * CAULDRON_LEVEL_MB;
                }
            }
        }
        // Pass 2: place a single source block in adjacent air per full bucket.
        for (int[] off : OFFSETS) {
            if (remaining < BUCKET_MB) break;
            Block b = origin.getRelative(off[0], off[1], off[2]);
            if (b.getType().isAir()) {
                b.setType(targetSource, false);
                remaining -= BUCKET_MB;
            }
        }
        return stack.amountMb() - remaining;
    }
}
