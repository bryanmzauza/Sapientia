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
 * Quarry-controller stub (T-431 / 1.7.0). 3×3×4 hollow shell of
 * {@code sapientia:stainless_steel_casing} (or {@code minecraft:iron_block} as a
 * vanilla proxy — stand-in for the future carbon-steel casing).
 *
 * <p>For 1.7.0 the controller is a placement + shape-validation stub like the
 * 1.4.0 induction furnace and 1.5.0 oil refinery: the shape is checked and the
 * player gets feedback sounds, but the AABB-driven mining tick lands in 1.7.1
 * along with the wrench-driven AABB selector ({@code T-440}).
 */
public final class SapientiaQuarryController implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaQuarryController(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "quarry_controller");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.LODESTONE; }
    @Override public @NotNull String displayNameKey() { return "block.quarry_controller.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        boolean valid = MultiblockShapeValidator.validateHollowBox(
                event.block(), 3, 3, 4,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK);
        if (valid) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        }
    }
}
