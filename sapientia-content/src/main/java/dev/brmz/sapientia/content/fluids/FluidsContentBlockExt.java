package dev.brmz.sapientia.content.fluids;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Cross-package extension of {@link FluidsContentBlock} so the {@code gas}
 * subpackage can declare new fluid-graph blocks (T-426 / 1.6.0) without
 * exposing the package-private base.
 */
public abstract class FluidsContentBlockExt implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final FluidNodeType nodeType;
    private final EnergyTier tier;

    protected FluidsContentBlockExt(@NotNull Plugin plugin, @NotNull String name, @NotNull Material material,
                                    @NotNull String displayKey, @NotNull FluidNodeType nodeType,
                                    @NotNull EnergyTier tier) {
        this.id = new NamespacedKey(plugin, name);
        this.material = material;
        this.displayKey = displayKey;
        this.nodeType = nodeType;
        this.tier = tier;
    }

    @Override public final @NotNull NamespacedKey id() { return id; }
    @Override public final @NotNull Material baseMaterial() { return material; }
    @Override public final @NotNull String displayNameKey() { return displayKey; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        Sapientia.get().fluids().addNode(event.block(), nodeType, tier);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().fluids().removeNode(event.block());
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        // Inspection handled by /sapientia fluids info.
    }
}
