package dev.brmz.sapientia.content.petroleum;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Refinery-grade stainless-steel casing (T-413 / 1.5.0). Used as the shell
 * material of the 5×5×7 oil-refinery multiblock.
 */
public final class SapientiaStainlessSteelCasing implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaStainlessSteelCasing(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "stainless_steel_casing");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.LIGHT_GRAY_GLAZED_TERRACOTTA; }
    @Override public @NotNull String displayNameKey() { return "block.stainless_steel_casing.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MATERIAL; }
}
