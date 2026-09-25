package dev.brmz.sapientia.api.agriculture;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where wild seeds of a plant are found: breaking one of the host blocks
 * (grass, ferns) in one of the biomes may drop a seed.
 *
 * @param hosts        blocks that can drop the seed
 * @param biomes       biome keys; empty means any biome
 * @param chance       chance per break, 0..1
 * @param requiredTool Sapientia item that must be held (for example a flint knife), or {@code null}
 */
public record WildSeedSource(
        @NotNull Set<Material> hosts,
        @NotNull Set<NamespacedKey> biomes,
        double chance,
        @Nullable NamespacedKey requiredTool) {

    public WildSeedSource {
        if (hosts.isEmpty()) throw new IllegalArgumentException("hosts must not be empty");
        if (chance < 0 || chance > 1) throw new IllegalArgumentException("chance must be 0..1");
        hosts = Set.copyOf(hosts);
        biomes = Set.copyOf(biomes);
    }
}
