package dev.brmz.sapientia.content.multiblock;

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
 * Induction-furnace controller (T-405 / 1.4.0). 3×3×3 casing of
 * {@code sapientia:invar_casing} or {@code minecraft:smooth_basalt} centered on
 * this block. On interact, the controller validates its shape via
 * {@link MultiblockShapeValidator#validateSolidCube} and reports the result to
 * the player through the standard Messages API.
 *
 * <p>For 1.4.0 the controller is a stub: shape validation works, but the
 * recipe-processing (steel, invar, kanthal smelting) lands in 1.4.1. The block
 * does not yet register as an energy node — that is added when the recipe
 * processing logic is wired in.
 */
public final class SapientiaInductionFurnaceController implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaInductionFurnaceController(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "induction_furnace_controller");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.SMITHING_TABLE; }
    @Override public @NotNull String displayNameKey() { return "block.induction_furnace_controller.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        boolean valid = MultiblockShapeValidator.validateSolidCube(
                event.block(), 3, Material.SMOOTH_BASALT, Material.POLISHED_BLACKSTONE);
        if (valid) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        }
    }
}
