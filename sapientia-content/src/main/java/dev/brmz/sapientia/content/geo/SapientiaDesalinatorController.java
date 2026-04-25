package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.multiblock.MultiblockShapeValidator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Desalinator-controller stub (T-434 / 1.7.0). 5×3×3 hollow shell of
 * {@code sapientia:stainless_steel_casing} (or {@code minecraft:iron_block} as a
 * vanilla proxy). Shape validation only in 1.7.0; the sea-water → fresh-water +
 * rock-salt processing tick lands in 1.7.1 alongside the brine fluid recipes.
 */
public final class SapientiaDesalinatorController implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaDesalinatorController(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "desalinator_controller");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.CONDUIT; }
    @Override public @NotNull String displayNameKey() { return "block.desalinator_controller.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        boolean valid = MultiblockShapeValidator.validateHollowBox(
                event.block(), 5, 3, 3,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK);
        if (valid) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        }
    }
}
