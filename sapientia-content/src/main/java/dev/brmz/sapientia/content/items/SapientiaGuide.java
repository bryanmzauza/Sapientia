package dev.brmz.sapientia.content.items;

import java.util.List;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The Sapientia Guide (T-150 / 0.4.0). Right-click opens the guide UI served by
 * {@link dev.brmz.sapientia.api.guide.GuideService} in the core.
 */
public final class SapientiaGuide implements SapientiaItem {

    private final NamespacedKey id;

    public SapientiaGuide(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "guide");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.WRITTEN_BOOK; }
    @Override public @NotNull String displayNameKey() { return "item.guide.name"; }
    @Override public @NotNull List<String> loreKeys() { return List.of("item.guide.lore"); }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.TOOL; }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Sapientia.get().guide().open(event.player());
    }
}
