package dev.brmz.sapientia.content.geo;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.multiblock.MultiblockShapeValidator;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Drill-rig controller stub (T-432 / 1.7.0). 5×5×8 hollow shell of
 * {@code sapientia:stainless_steel_casing} (or {@code minecraft:iron_block} as a
 * vanilla proxy). Shape validation only in 1.7.0; the sub-bedrock virtual-mining
 * tick with probability tables lands in 1.7.1.
 */
public final class SapientiaDrillRigController implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaDrillRigController(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "drill_rig_controller");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.OBSERVER; }
    @Override public @NotNull String displayNameKey() { return "block.drill_rig_controller.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        boolean valid = MultiblockShapeValidator.validateHollowBox(
                event.block(), 5, 5, 8,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK);
        if (valid) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
        }
    }

    /** Register the controller as an HV CONSUMER so the 1.7.1 GeoTicker can drive it. */
    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        long bufferMax = EnergySpecs.bufferMax(EnergyNodeType.CONSUMER, EnergyTier.HIGH);
        Sapientia.get().energy().addNode(event.block(), EnergyNodeType.CONSUMER, EnergyTier.HIGH, bufferMax);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().energy().removeNode(event.block());
    }
}
