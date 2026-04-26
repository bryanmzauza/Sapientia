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
 * Shared scaffolding for {@link SapientiaBlock} implementations that contribute
 * a fluid logistics node (T-301 / 1.2.0). Mirrors {@code LogisticsContentBlock}.
 */
abstract class FluidsContentBlock implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final FluidNodeType nodeType;
    private final EnergyTier tier;

    FluidsContentBlock(@NotNull Plugin plugin, @NotNull String name, @NotNull Material material,
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
    @Override public final @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }

    public final @NotNull FluidNodeType nodeType() { return nodeType; }
    public final @NotNull EnergyTier tier() { return tier; }

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
        // No UI in 1.2.0; tank inspection handled by /sapientia fluids info.
    }
}
