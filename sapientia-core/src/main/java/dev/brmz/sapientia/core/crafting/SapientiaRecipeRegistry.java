package dev.brmz.sapientia.core.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.RecipeRegistry;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.api.overrides.RecipeOverride;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory {@link RecipeRegistry} implementation (T-131 / 0.4.0). Matching is
 * shape-exact: each of the nine grid slots must match the corresponding cell in
 * the pattern with at least the required amount. Vanilla and Sapientia
 * ingredients are distinguished via the {@link ItemRegistry} PDC tag.
 */
public final class SapientiaRecipeRegistry implements RecipeRegistry {

    private final ItemRegistry itemRegistry;
    private final Map<NamespacedKey, SapientiaRecipe> recipes = new LinkedHashMap<>();
    private WorkbenchListener workbench;
    private @Nullable ContentOverrides overrides;

    public SapientiaRecipeRegistry(@NotNull ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /** Injects the override source so recipe results can be retuned via YAML (T-160). */
    public void setOverrides(@Nullable ContentOverrides overrides) {
        this.overrides = overrides;
    }

    /**
     * Returns the recipe's result stack with any operator override applied
     * (currently just the output quantity). Always returns a defensive clone.
     */
    public @NotNull ItemStack effectiveResult(@NotNull SapientiaRecipe recipe) {
        ItemStack result = recipe.result().clone();
        if (overrides != null) {
            RecipeOverride ov = overrides.forRecipe(recipe.id()).orElse(null);
            if (ov != null && ov.resultAmount().isPresent()) {
                result.setAmount(ov.resultAmount().get());
            }
        }
        return result;
    }

    /** Installs the workbench listener used by {@link #openWorkbench(Player)}. */
    public void attachWorkbench(@NotNull WorkbenchListener workbench) {
        this.workbench = workbench;
    }

    @Override
    public void openWorkbench(@NotNull Player player) {
        if (workbench == null) {
            throw new IllegalStateException("Workbench listener not attached yet.");
        }
        workbench.open(player);
    }

    @Override
    public void register(@NotNull SapientiaRecipe recipe) {
        List<RecipeIngredient> pattern = recipe.pattern();
        if (pattern == null || pattern.size() != 9) {
            throw new IllegalArgumentException("Recipe pattern must have exactly 9 cells: " + recipe.id());
        }
        if (recipes.putIfAbsent(recipe.id(), recipe) != null) {
            throw new IllegalStateException("Duplicate Sapientia recipe id: " + recipe.id());
        }
    }

    @Override
    public @NotNull Optional<SapientiaRecipe> find(@NotNull NamespacedKey id) {
        return Optional.ofNullable(recipes.get(id));
    }

    @Override
    public @NotNull Collection<SapientiaRecipe> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(recipes.values()));
    }

    @Override
    public @NotNull Optional<SapientiaRecipe> match(@NotNull @Nullable ItemStack[] grid) {
        if (grid == null || grid.length != 9) return Optional.empty();
        MatchCell[] cells = new MatchCell[9];
        for (int i = 0; i < 9; i++) cells[i] = toCell(grid[i]);
        return matchCells(cells);
    }

    /**
     * Bukkit-free overload used by unit tests: caller supplies already-extracted
     * {@link MatchCell} values (one per grid slot, {@code null} = empty).
     */
    @NotNull Optional<SapientiaRecipe> matchCells(@NotNull MatchCell @NotNull [] cells) {
        if (cells.length != 9) return Optional.empty();
        for (SapientiaRecipe recipe : recipes.values()) {
            if (matches(recipe.pattern(), cells)) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    private boolean matches(List<RecipeIngredient> pattern, MatchCell[] cells) {
        for (int i = 0; i < 9; i++) {
            if (!cellMatches(pattern.get(i), cells[i])) return false;
        }
        return true;
    }

    private boolean cellMatches(RecipeIngredient ingredient, @Nullable MatchCell cell) {
        boolean empty = cell == null || cell.amount() == 0;
        if (ingredient instanceof RecipeIngredient.Empty) return empty;
        if (empty) return false;
        if (ingredient instanceof RecipeIngredient.Vanilla v) {
            if (cell.sapientiaId() != null) return false;
            return cell.material() == v.material() && cell.amount() >= v.amount();
        }
        if (ingredient instanceof RecipeIngredient.Sapientia s) {
            return s.id().toString().equals(cell.sapientiaId()) && cell.amount() >= s.amount();
        }
        return false;
    }

    private @Nullable MatchCell toCell(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() == 0) return null;
        return new MatchCell(stack.getType(), itemRegistry.idOf(stack), stack.getAmount());
    }

    /** Bukkit-free snapshot of one crafting grid slot. */
    record MatchCell(@NotNull org.bukkit.Material material, @Nullable String sapientiaId, int amount) {}
}
