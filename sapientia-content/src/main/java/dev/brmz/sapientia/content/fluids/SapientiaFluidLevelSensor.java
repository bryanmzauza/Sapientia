package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fluid level sensor (T-443 / 1.8.0). Emits a redstone-ish signal proportional
 * to the fill ratio of an adjacent {@code fluid_tank}, exposed to the logic
 * runtime (T-302) as a sensor input. Placement-only in 1.8.0; per-tick read
 * lands with the logic-runtime hookup in 1.8.1.
 */
public final class SapientiaFluidLevelSensor implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaFluidLevelSensor(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "fluid_level_sensor");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.COMPARATOR; }
    @Override public @NotNull String displayNameKey() { return "block.fluid_level_sensor.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
