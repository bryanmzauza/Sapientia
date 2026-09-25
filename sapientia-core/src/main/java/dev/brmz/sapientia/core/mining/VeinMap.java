package dev.brmz.sapientia.core.mining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SplittableRandom;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralSource;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mineral veins derived from the world seed. The world is split into square
 * regions of {@code regionChunks} chunks; each region holds a vein with
 * probability {@code veinChance}, of a mineral picked by weight among the
 * minerals found in that dimension. Pure function of seed and coordinates:
 * no storage, same answer on every restart and for prospecting later.
 */
public final class VeinMap {

    private record Weighted(Mineral mineral, int weight) {}

    private final int regionChunks;
    private final double veinChance;
    private final List<Weighted>[] byEnvironment;
    private final int[] totals;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public VeinMap(@NotNull Collection<Mineral> minerals, int regionChunks, double veinChance) {
        this.regionChunks = Math.max(1, regionChunks);
        this.veinChance = veinChance;
        World.Environment[] environments = World.Environment.values();
        this.byEnvironment = new List[environments.length];
        this.totals = new int[environments.length];
        for (World.Environment env : environments) {
            List<Weighted> list = new ArrayList<>();
            for (Mineral mineral : minerals) {
                int weight = 0;
                for (MineralSource source : mineral.sources()) {
                    if (source.environment() == null || source.environment() == env) weight += source.weight();
                }
                if (weight > 0) {
                    list.add(new Weighted(mineral, weight));
                    totals[env.ordinal()] += weight;
                }
            }
            byEnvironment[env.ordinal()] = list;
        }
    }

    /** The vein mineral covering a chunk, or {@code null} if its region has no vein. */
    public @Nullable Mineral veinAt(long seed, @NotNull World.Environment environment, int chunkX, int chunkZ) {
        List<Weighted> list = byEnvironment[environment.ordinal()];
        int total = totals[environment.ordinal()];
        if (list.isEmpty() || total <= 0) return null;
        long rx = Math.floorDiv(chunkX, regionChunks);
        long rz = Math.floorDiv(chunkZ, regionChunks);
        long h = seed ^ (rx * 0x9E3779B97F4A7C15L) ^ (rz * 0xC2B2AE3D27D4EB4FL) ^ ((long) environment.ordinal() << 56);
        SplittableRandom random = new SplittableRandom(h);
        if (random.nextDouble() >= veinChance) return null;
        int pick = random.nextInt(total);
        for (Weighted w : list) {
            pick -= w.weight();
            if (pick < 0) return w.mineral();
        }
        return list.get(list.size() - 1).mineral();
    }
}
