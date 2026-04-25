package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Comparator sensor (T-441 / 1.8.0). Emits a redstone-ish signal proportional
 * to the fill level of an adjacent container, exposed to logic programs
 * (T-302) as a {@code COMPARATOR} input source. 1.8.0 ships the placement +
 * registration; the per-tick read is wired to the logic runtime in 1.8.1.
 */
public final class SapientiaComparatorSensor implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaComparatorSensor(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "comparator_sensor");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.COMPARATOR; }
    @Override public @NotNull String displayNameKey() { return "block.comparator_sensor.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
