package dev.brmz.sapientia.content.items;

import java.util.List;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The Era Almanac: right-click lists every era and what each one brings, served
 * by {@link dev.brmz.sapientia.api.guide.GuideService#openEras(Player)}. Uses a
 * knowledge book for the same reason as {@link SapientiaGuide}.
 */
public final class SapientiaEraAlmanac implements CatalogItem {

    private final Plugin plugin;
    private final NamespacedKey id;

    public SapientiaEraAlmanac(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.id = new NamespacedKey(plugin, "era_almanac");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.KNOWLEDGE_BOOK; }
    @Override public @NotNull String displayNameKey() { return "item.era_almanac.name"; }
    @Override public @NotNull List<String> loreKeys() { return List.of("item.era_almanac.lore"); }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.TOOL; }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Player player = event.player();
        Bukkit.getScheduler().runTask(plugin, () -> Sapientia.get().guide().openEras(player));
    }
}
