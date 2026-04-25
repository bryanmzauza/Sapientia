package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * GPS transmitter (T-436 / 1.7.0). Broadcasts a coverage signal that the GPS
 * handheld map and prospector pick up to render way-point markers. In 1.7.0
 * the transmitter is a placement-only block; the coverage radius scan,
 * persistence and overlay rendering land in 1.7.1.
 */
public final class SapientiaGpsTransmitter implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaGpsTransmitter(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "gps_transmitter");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.BEACON; }
    @Override public @NotNull String displayNameKey() { return "block.gps_transmitter.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
}
