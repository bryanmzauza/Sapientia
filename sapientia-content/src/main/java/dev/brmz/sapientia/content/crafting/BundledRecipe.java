package dev.brmz.sapientia.content.crafting;

import java.util.List;

import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Small record holding a fully-defined Sapientia recipe (T-131 / 0.4.0). Used by
 * the bundled catalog in {@link BundledRecipes}.
 */
public record BundledRecipe(
        @NotNull NamespacedKey id,
        @NotNull List<RecipeIngredient> pattern,
        @NotNull ItemStack result,
        @NotNull GuideCategory category) implements SapientiaRecipe {

    public BundledRecipe {
        if (pattern.size() != 9) {
            throw new IllegalArgumentException("pattern must have 9 cells");
        }
        pattern = List.copyOf(pattern);
    }

    public static @NotNull BundledRecipe of(
            @NotNull NamespacedKey id,
            @NotNull List<RecipeIngredient> pattern,
            @NotNull Material resultMaterial,
            @NotNull GuideCategory category) {
        return new BundledRecipe(id, pattern, new ItemStack(resultMaterial), category);
    }
}
