package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Shared scaffolding for {@link SapientiaBlock} implementations that contribute
 * an item logistics node (T-300 / 1.1.0). Mirrors {@code EnergyContentBlock}.
 *
 * <p>Item buffers live in adjacent vanilla containers, so this base class has
 * no buffer of its own — it only registers/unregisters the node with the
 * {@code ItemService}.
 */
abstract class LogisticsContentBlock implements SapientiaBlock {

    private final NamespacedKey id;
    private final Material material;
    private final String displayKey;
    private final ItemNodeType nodeType;
    private final EnergyTier tier;
    private final int priority;

    LogisticsContentBlock(
            @NotNull Plugin plugin, @NotNull String name, @NotNull Material material,
            @NotNull String displayKey, @NotNull ItemNodeType nodeType,
            @NotNull EnergyTier tier, int priority) {
        this.id = new NamespacedKey(plugin, name);
        this.material = material;
        this.displayKey = displayKey;
        this.nodeType = nodeType;
        this.tier = tier;
        this.priority = priority;
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

    @Override
    public final @NotNull GuideCategory guideCategory() {
        return GuideCategory.LOGISTICS;
    }

    public final @NotNull ItemNodeType nodeType() {
        return nodeType;
    }

    public final @NotNull EnergyTier tier() {
        return tier;
    }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        Sapientia.get().logistics().addNode(event.block(), nodeType, tier, priority);
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Sapientia.get().logistics().removeNode(event.block());
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        // Only filter nodes have a UI; subclasses override if needed.
    }
}
