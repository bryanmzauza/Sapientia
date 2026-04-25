package dev.brmz.sapientia.api.crafting;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of shaped {@link SapientiaRecipe}s (T-131 / 0.4.0). Addons register
 * recipes on enable; the Sapientia workbench queries {@link #match(ItemStack[])}
 * with its 3&times;3 grid contents on every grid change.
 */
public interface RecipeRegistry {

    /** Registers a recipe. Throws if another recipe already claimed the same id. */
    void register(@NotNull SapientiaRecipe recipe);

    /** Looks up a recipe by its stable id. */
    @NotNull Optional<SapientiaRecipe> find(@NotNull NamespacedKey id);

    /** All registered recipes, in insertion order. */
    @NotNull Collection<SapientiaRecipe> all();

    /**
     * Finds the first recipe whose pattern matches the given 3&times;3 grid
     * (row-major, nine slots, {@code null} or air entries are treated as empty).
     */
    @NotNull Optional<SapientiaRecipe> match(@NotNull @Nullable ItemStack[] grid);

    /** Opens the Sapientia workbench UI for the given player (T-130 / 0.4.0). */
    void openWorkbench(@NotNull Player player);
}
