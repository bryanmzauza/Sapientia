package dev.brmz.sapientia.content.petroleum;

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
 * Oil-refinery controller (T-413 / 1.5.0). Validates a 5×5×7 hollow shell of
 * {@code sapientia:stainless_steel_casing} (or {@code minecraft:iron_block} as a
 * vanilla proxy) centered on this block.
 *
 * <p>For 1.5.0 the controller is a stub like the induction-furnace controller:
 * shape validation works and the player gets feedback sounds, but the refinery
 * tick (crude → diesel + gasoline + lubricant + tar) is deferred to 1.5.1. The
 * block does not yet register as a fluid node — that is added when the recipe
 * pipeline is wired in. See {@code docs/content-spec-T-41x.md}.
 */
public final class SapientiaOilRefineryController implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaOilRefineryController(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "oil_refinery_controller");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.LOOM; }
    @Override public @NotNull String displayNameKey() { return "block.oil_refinery_controller.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        boolean valid = MultiblockShapeValidator.validateHollowBox(
                event.block(), 5, 5, 7,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK);
        if (valid) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        }
    }
}
