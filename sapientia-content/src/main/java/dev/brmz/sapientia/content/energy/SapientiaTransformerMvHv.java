package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * MV ↔ HV transformer (T-425 / 1.6.0). Modeled as a single HV-tier capacitor
 * node bridging adjacent MV networks; mirrors {@link SapientiaTransformerLvMv}.
 */
public final class SapientiaTransformerMvHv implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaTransformerMvHv(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "transformer_mv_hv");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.SOUL_LANTERN; }
    @Override public @NotNull String displayNameKey() { return "block.transformer_mv_hv.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.ENERGY; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        long bufferMax = EnergySpecs.bufferMax(EnergyNodeType.CAPACITOR, EnergyTier.HIGH);
        Sapientia.get().energy().addNode(event.block(), EnergyNodeType.CAPACITOR, EnergyTier.HIGH, bufferMax);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().energy().removeNode(event.block());
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Sapientia.get().energy().nodeAt(event.block())
                .ifPresent(node -> Sapientia.get().openMachineUI(event.player(), node));
    }
}
