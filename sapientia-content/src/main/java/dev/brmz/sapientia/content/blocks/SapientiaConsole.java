package dev.brmz.sapientia.content.blocks;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Interactive demo block (T-180). Right-clicking plays a sound; a real UI will be
 * hooked in once the 0.3.0 UI services (T-140) expose the open-by-id entry point
 * through the public API.
 */
public final class SapientiaConsole implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaConsole(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "console");
    }

    @Override
    public @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public @NotNull Material baseMaterial() {
        return Material.LODESTONE;
    }

    @Override
    public @NotNull String displayNameKey() {
        return "block.console.name";
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.8f);
    }
}
