package dev.brmz.sapientia.core.mining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralSource;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drop tables compiled per host block: breaking a host rolls its base chance
 * (raised by Fortune and by a vein of a mineral the host can hold), then picks
 * a mineral among those whose sources accept the height, biome and dimension.
 * Era and tool checks happen afterwards, so a locked mineral still consumes
 * the roll (mining early wastes the resources of later eras).
 */
public final class FragmentRoller {

    /** Fortune I, II and III multiply the chance by these values. */
    private static final double[] FORTUNE = {1.0, 1.3, 1.6, 2.0};
    /** Inside a vein, this share of drops is the vein's mineral. */
    private static final double VEIN_SHARE = 2.0 / 3.0;

    private record Candidate(Mineral mineral, MineralSource source) {}

    private final Map<Material, List<Candidate>> byHost = new EnumMap<>(Material.class);
    private final Map<Material, Double> hostChance;
    private final double veinMultiplier;

    public FragmentRoller(@NotNull Collection<Mineral> minerals, @NotNull Map<Material, Double> hostChance,
                          double veinMultiplier) {
        this.hostChance = Map.copyOf(hostChance);
        this.veinMultiplier = veinMultiplier;
        for (Mineral mineral : minerals) {
            for (MineralSource source : mineral.sources()) {
                for (Material host : source.hosts()) {
                    byHost.computeIfAbsent(host, k -> new ArrayList<>()).add(new Candidate(mineral, source));
                }
            }
        }
    }

    /** Blocks that can drop a fragment. */
    public @NotNull Set<Material> hosts() {
        return byHost.keySet();
    }

    /**
     * Rolls one break. Returns the mineral that drops, before era and tool
     * checks, or {@code null} when nothing drops.
     */
    public @Nullable Mineral roll(@NotNull Material host, int y, @Nullable NamespacedKey biome,
                                  @NotNull World.Environment environment, @Nullable Mineral vein,
                                  int fortune, @NotNull RandomGenerator random) {
        Double base = hostChance.get(host);
        List<Candidate> candidates = byHost.get(host);
        if (base == null || base <= 0 || candidates == null) return null;
        List<Candidate> eligible = new ArrayList<>(candidates.size());
        int totalWeight = 0;
        boolean veinHere = false;
        for (Candidate c : candidates) {
            if (!c.source().accepts(y, biome, environment)) continue;
            eligible.add(c);
            totalWeight += c.source().weight();
            if (c.mineral().equals(vein)) veinHere = true;
        }
        if (eligible.isEmpty()) return null;
        double chance = base * FORTUNE[Math.max(0, Math.min(FORTUNE.length - 1, fortune))];
        if (veinHere) chance *= veinMultiplier;
        if (random.nextDouble() >= chance) return null;
        if (veinHere && random.nextDouble() < VEIN_SHARE) return vein;
        int pick = random.nextInt(totalWeight);
        for (Candidate c : eligible) {
            pick -= c.source().weight();
            if (pick < 0) return c.mineral();
        }
        return eligible.get(eligible.size() - 1).mineral();
    }
}
