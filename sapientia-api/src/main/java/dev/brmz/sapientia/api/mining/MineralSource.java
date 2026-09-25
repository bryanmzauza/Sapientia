package dev.brmz.sapientia.api.mining;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where a mineral can drop: natural host blocks within a height range,
 * optionally limited to some biomes and one dimension.
 *
 * @param hosts       natural blocks that can hold the mineral
 * @param minY        lowest block Y, inclusive
 * @param maxY        highest block Y, inclusive
 * @param biomes      biome keys the source is limited to; empty means any biome
 * @param environment dimension the source is limited to; {@code null} means any
 * @param weight      relative chance against the other minerals of the same host
 */
public record MineralSource(
        @NotNull Set<Material> hosts,
        int minY,
        int maxY,
        @NotNull Set<NamespacedKey> biomes,
        @Nullable World.Environment environment,
        int weight) {

    public MineralSource {
        if (hosts.isEmpty()) throw new IllegalArgumentException("hosts must not be empty");
        if (minY > maxY) throw new IllegalArgumentException("minY must not exceed maxY");
        if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
        hosts = Set.copyOf(hosts);
        biomes = Set.copyOf(biomes);
    }

    /** Whether a block at {@code y}, in {@code biome} and {@code environment}, can yield this source. */
    public boolean accepts(int y, @Nullable NamespacedKey biome, @NotNull World.Environment environment) {
        if (y < minY || y > maxY) return false;
        if (this.environment != null && this.environment != environment) return false;
        return biomes.isEmpty() || (biome != null && biomes.contains(biome));
    }
}
