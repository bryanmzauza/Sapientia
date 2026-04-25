package dev.brmz.sapientia.content.blocks;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Purely decorative Sapientia block. Proves the block registration + persistence
 * path end-to-end without any custom logic (T-180).
 */
public final class SapientiaPedestal implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaPedestal(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "pedestal");
    }

    @Override
    public @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public @NotNull Material baseMaterial() {
        return Material.SMOOTH_STONE;
    }

    @Override
    public @NotNull String displayNameKey() {
        return "block.pedestal.name";
    }
}
