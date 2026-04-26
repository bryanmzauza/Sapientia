package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Unpackager (T-441 / 1.8.0). Inverse of {@link SapientiaPackager}: explodes a
 * {@code packaged_bundle} back into its component stacks into an adjacent
 * container. Placement-only in 1.8.0; the actual NBT unpack tick lands with
 * the routing rework in 1.8.1.
 */
public final class SapientiaUnpackager implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaUnpackager(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "unpackager");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.DISPENSER; }
    @Override public @NotNull String displayNameKey() { return "block.unpackager.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
