package dev.brmz.sapientia.content.mining;

import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * The fragment of a mineral (dropped by natural rock) or its tailings (left by
 * separation). Both belong to the mineral's era.
 */
public final class MineralItem implements SapientiaItem {

    /** Which of the two items of a mineral this is. */
    public enum Kind { FRAGMENT, TAILINGS }

    private final NamespacedKey id;
    private final Era era;
    private final Kind kind;

    public MineralItem(@NotNull Mineral mineral, @NotNull Kind kind) {
        this.id = kind == Kind.FRAGMENT ? mineral.fragmentItem() : mineral.tailingsItem();
        this.era = mineral.era();
        this.kind = kind;
    }

    @Override
    public @NotNull NamespacedKey id() {
        return id;
    }

    @Override
    public @NotNull Material baseMaterial() {
        // Neither can be placed; vanilla crafting with Sapientia items is blocked by the core.
        return kind == Kind.FRAGMENT ? Material.FLINT : Material.CLAY_BALL;
    }

    @Override
    public @NotNull String displayNameKey() {
        return "mineral." + id.getKey() + ".name";
    }

    @Override
    public @NotNull GuideCategory guideCategory() {
        return GuideCategory.MATERIAL;
    }

    @Override
    public @NotNull Era era() {
        return era;
    }

    public @NotNull Kind kind() {
        return kind;
    }
}
