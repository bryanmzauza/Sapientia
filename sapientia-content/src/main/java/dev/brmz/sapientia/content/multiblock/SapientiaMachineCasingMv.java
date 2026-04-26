package dev.brmz.sapientia.content.multiblock;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier shared machine casing (T-400 / T-405 / 1.4.0). Used in MV machines + induction furnace. */
public final class SapientiaMachineCasingMv implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaMachineCasingMv(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "machine_casing_mv");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.SMOOTH_BASALT; }
    @Override public @NotNull String displayNameKey() { return "block.machine_casing_mv.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MATERIAL; }
}
