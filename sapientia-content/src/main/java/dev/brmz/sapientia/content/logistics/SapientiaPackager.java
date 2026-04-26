package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Packager (T-441 / 1.8.0). Bundles the items in an adjacent container into a
 * single {@code packaged_bundle} ItemStack with a Sapientia NBT payload, so a
 * fixed recipe pattern (e.g. solar-panel kit) ships as one stack through the
 * logistics network. Placement-only in 1.8.0; the NBT format is locked by
 * ADR-020 (deferred to 1.8.1) and the packaging tick lands with it.
 *
 * <p>Companion: {@link SapientiaUnpackager}.
 */
public final class SapientiaPackager implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaPackager(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "packager");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.DROPPER; }
    @Override public @NotNull String displayNameKey() { return "block.packager.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
