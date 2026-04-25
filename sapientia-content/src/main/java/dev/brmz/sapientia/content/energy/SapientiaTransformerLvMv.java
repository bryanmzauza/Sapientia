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
 * LV ↔ MV transformer (T-406 / 1.4.0). Acts as two paired energy nodes — a
 * low-voltage capacitor face and a medium-voltage capacitor face — that route
 * energy bidirectionally between the two networks at the lower throughput.
 *
 * <p>For 1.4.0 we model the transformer as a single MV-tier capacitor node with
 * a documented role of bridging adjacent LV networks. A future revision (1.5.0)
 * will add a true paired-node implementation backed by two
 * {@link dev.brmz.sapientia.api.energy.EnergyService#addNode} entries.
 */
public final class SapientiaTransformerLvMv implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaTransformerLvMv(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "transformer_lv_mv");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.LIGHTNING_ROD; }
    @Override public @NotNull String displayNameKey() { return "block.transformer_lv_mv.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.ENERGY; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        long bufferMax = EnergySpecs.bufferMax(EnergyNodeType.CAPACITOR, EnergyTier.MID);
        Sapientia.get().energy().addNode(event.block(), EnergyNodeType.CAPACITOR, EnergyTier.MID, bufferMax);
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
