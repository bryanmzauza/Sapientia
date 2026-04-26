package dev.brmz.sapientia.content.items;

import java.util.List;

import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Inspector tool. Just aiming at a Sapientia energy block while holding the
 * wrench renders the live readout via the {@code EnergyInspector} look-scan —
 * no click is needed. Clicking plays a short UI sound for feedback.
 */
public final class SapientiaWrench implements SapientiaItem {

    private final NamespacedKey id;

    public SapientiaWrench(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "wrench");
    }

    @Override
    public @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public @NotNull Material baseMaterial() {
        return Material.STICK;
    }

    @Override
    public @NotNull String displayNameKey() {
        return "item.wrench.name";
    }

    @Override
    public @NotNull List<String> loreKeys() {
        return List.of("item.wrench.lore");
    }

    @Override
    public void onUse(@NotNull SapientiaItemInteractEvent event) {
        Player player = event.player();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }
}
