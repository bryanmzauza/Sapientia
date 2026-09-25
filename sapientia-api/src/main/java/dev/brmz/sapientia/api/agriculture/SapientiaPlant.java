package dev.brmz.sapientia.api.agriculture;

import java.util.List;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A Sapientia plant. In the world it grows as a vanilla crop block
 * ({@link #crop()}) with vanilla growth, so it costs nothing while growing;
 * the plugin only remembers which crops are Sapientia plants and replaces the
 * drops at harvest.
 *
 * <p>The seed item's base material must be the vanilla item that plants
 * {@link #crop()} (for example {@code WHEAT_SEEDS} for {@code WHEAT}).
 *
 * @param id          plant id
 * @param era         era from which the plant can be planted and its wild seeds found
 * @param seedItem    Sapientia item that plants it
 * @param produceItem Sapientia item harvested when mature
 * @param crop        vanilla crop block used in the world (must be ageable)
 * @param produceMin  produce per mature harvest, minimum
 * @param produceMax  produce per mature harvest, maximum
 * @param seedsMin    seeds returned per mature harvest, minimum
 * @param seedsMax    seeds returned per mature harvest, maximum
 * @param wildSources where wild seeds drop
 */
public record SapientiaPlant(
        @NotNull NamespacedKey id,
        @NotNull Era era,
        @NotNull NamespacedKey seedItem,
        @NotNull NamespacedKey produceItem,
        @NotNull Material crop,
        int produceMin,
        int produceMax,
        int seedsMin,
        int seedsMax,
        @NotNull List<WildSeedSource> wildSources) {

    public SapientiaPlant {
        if (produceMin < 0 || produceMax < produceMin) throw new IllegalArgumentException("bad produce range");
        if (seedsMin < 0 || seedsMax < seedsMin) throw new IllegalArgumentException("bad seed range");
        wildSources = List.copyOf(wildSources);
    }
}
