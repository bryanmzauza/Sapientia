package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNode;
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
 * Public scaffolding for energy-bearing machine blocks (T-404 / 1.4.0).
 * Equivalent to the package-private {@code EnergyContentBlock} but defaults
 * to the {@link GuideCategory#MACHINE} guide category and is open for cross-package
 * subclassing in the {@code metallurgy} and {@code machines} subpackages.
 */
public abstract class MachineEnergyBlock implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final EnergyNodeType type;
    private final EnergyTier tier;

    protected MachineEnergyBlock(
            @NotNull Plugin plugin, @NotNull String name, @NotNull Material material,
            @NotNull String displayKey, @NotNull EnergyNodeType type, @NotNull EnergyTier tier) {
        this.id = new NamespacedKey(plugin, name);
        this.material = material;
        this.displayKey = displayKey;
        this.type = type;
        this.tier = tier;
    }

    @Override public final @NotNull NamespacedKey id() { return id; }
    @Override public final @NotNull Material baseMaterial() { return material; }
    @Override public final @NotNull String displayNameKey() { return displayKey; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    public final @NotNull EnergyNodeType nodeType() { return type; }
    public final @NotNull EnergyTier tier() { return tier; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        long bufferMax = EnergySpecs.bufferMax(type, tier);
        Sapientia.get().energy().addNode(event.block(), type, tier, bufferMax);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().energy().removeNode(event.block());
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        if (type == EnergyNodeType.CABLE) return;
        EnergyNode node = Sapientia.get().energy().nodeAt(event.block()).orElse(null);
        if (node == null) return;
        Sapientia.get().openMachineUI(event.player(), node);
    }
}
