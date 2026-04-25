package dev.brmz.sapientia.content.energy;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Shared scaffolding for {@link SapientiaBlock} implementations that contribute an
 * energy node. Subclasses declare id, material, type and tier; the base class
 * registers / unregisters the node with the {@code EnergyService}.
 */
abstract class EnergyContentBlock implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final EnergyNodeType type;
    private final EnergyTier tier;

    EnergyContentBlock(
            @NotNull Plugin plugin, @NotNull String name, @NotNull Material material,
            @NotNull String displayKey, @NotNull EnergyNodeType type, @NotNull EnergyTier tier) {
        this.id = new NamespacedKey(plugin, name);
        this.material = material;
        this.displayKey = displayKey;
        this.type = type;
        this.tier = tier;
    }

    @Override
    public final @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public final @NotNull Material baseMaterial() {
        return material;
    }

    @Override
    public final @NotNull String displayNameKey() {
        return displayKey;
    }

    public final @NotNull EnergyNodeType nodeType() {
        return type;
    }

    public final @NotNull EnergyTier tier() {
        return tier;
    }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        long bufferMax = EnergySpecs.bufferMax(type, tier);
        Sapientia.get().energy().addNode(event.block(), type, tier, bufferMax);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().energy().removeNode(event.block());
    }
}
