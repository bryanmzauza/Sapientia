package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * GPS marker (T-436 / 1.7.0). A passive way-point block that registers a named
 * point of interest. The handheld map (1.7.1) renders these markers if the
 * player is inside a transmitter's coverage radius. Placement-only in 1.7.0.
 */
public final class SapientiaGpsMarker implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaGpsMarker(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "gps_marker");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.LIGHTNING_ROD; }
    @Override public @NotNull String displayNameKey() { return "block.gps_marker.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
