package dev.brmz.sapientia.api.crafting;

import java.util.List;

import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A shaped 3&times;3 recipe for the Sapientia workbench (T-131 / 0.4.0).
 *
 * <p>The pattern is row-major, nine elements long. Cells not occupied by an
 * ingredient must be set to {@link RecipeIngredient#empty()}. Matching is
 * shape-exact: rotations and translations are not considered. Addons that want
 * shapeless semantics should register multiple recipes.
 */
public interface SapientiaRecipe {

    /** Stable registry id, e.g. {@code sapientia:recipe/wrench}. */
    @NotNull NamespacedKey id();

    /** Row-major 3&times;3 ingredient pattern, exactly nine cells. */
    @NotNull List<RecipeIngredient> pattern();

    /** Result stack produced per craft. */
    @NotNull ItemStack result();

    /** Guide category this recipe is listed under. */
    default @NotNull GuideCategory category() {
        return GuideCategory.MISC;
    }
}
