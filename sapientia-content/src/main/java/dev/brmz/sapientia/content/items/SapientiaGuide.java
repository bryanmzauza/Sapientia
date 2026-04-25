package dev.brmz.sapientia.content.items;

import java.util.List;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The Sapientia Guide (T-150 / 0.4.0). Right-click opens the guide UI served by
 * {@link dev.brmz.sapientia.api.guide.GuideService} in the core.
 *
 * <p>Uses {@link Material#KNOWLEDGE_BOOK} instead of {@code WRITTEN_BOOK}: the
 * latter is opened client-side via a dedicated packet that survives
 * {@code PlayerInteractEvent#setCancelled(true)}, stomping on the custom
 * inventory we open. Knowledge books have no vanilla right-click behavior.
 */
public final class SapientiaGuide implements SapientiaItem {

    private final Plugin plugin;
    private final NamespacedKey id;

    public SapientiaGuide(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.id = new NamespacedKey(plugin, "guide");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.KNOWLEDGE_BOOK; }
    @Override public @NotNull String displayNameKey() { return "item.guide.name"; }
    @Override public @NotNull List<String> loreKeys() { return List.of("item.guide.lore"); }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.TOOL; }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Player player = event.player();
        // Deferring by 1 tick avoids the rare race where the vanilla item-use
        // pipeline closes windows that were opened mid-event on the same tick.
        Bukkit.getScheduler().runTask(plugin, () -> Sapientia.get().guide().open(player));
    }
}
