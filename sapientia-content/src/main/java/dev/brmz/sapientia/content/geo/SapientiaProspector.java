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
 * GPS-style prospector (T-433 / 1.7.0). Right-click sweeps the chunk for
 * sub-bedrock ore reservoirs and reports the strongest signal — but in 1.7.0
 * the scan is a stub feedback sound. The actual chunk-radius prospect logic
 * lands in 1.7.1 alongside the GPS coverage system.
 */
public final class SapientiaProspector implements SapientiaItem {

    private final NamespacedKey id;

    public SapientiaProspector(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "prospector");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.SPYGLASS; }
    @Override public @NotNull String displayNameKey() { return "item.prospector.name"; }
    @Override public @NotNull List<String> loreKeys() { return List.of("item.prospector.lore"); }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.TOOL; }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Player player = event.player();
        player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, 0.7f, 1.2f);
    }
}
