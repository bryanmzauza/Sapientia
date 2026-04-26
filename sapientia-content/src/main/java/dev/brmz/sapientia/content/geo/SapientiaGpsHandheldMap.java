package dev.brmz.sapientia.content.geo;

import java.util.List;

import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * GPS handheld map (T-436 / 1.7.0). Right-click renders nearby GPS markers if
 * the player is in transmitter coverage. In 1.7.0 the right-click is a stub
 * feedback sound — the marker query, coverage check and overlay land in 1.7.1
 * (T-440 Bedrock parity adds the {@code CustomForm} numeric editor for AABBs).
 */
public final class SapientiaGpsHandheldMap implements SapientiaItem {

    private final NamespacedKey id;

    public SapientiaGpsHandheldMap(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "gps_handheld_map");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.COMPASS; }
    @Override public @NotNull String displayNameKey() { return "item.gps_handheld_map.name"; }
    @Override public @NotNull List<String> loreKeys() { return List.of("item.gps_handheld_map.lore"); }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.TOOL; }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Player player = event.player();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
    }
}
