package dev.brmz.sapientia.content.multiblock;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier shared machine casing (T-400 / T-405 / 1.4.0). Used in machine and multiblock recipes. */
public final class SapientiaMachineCasing implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaMachineCasing(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "machine_casing");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.POLISHED_BLACKSTONE; }
    @Override public @NotNull String displayNameKey() { return "block.machine_casing.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MATERIAL; }
}
