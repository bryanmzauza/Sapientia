package dev.brmz.sapientia.core.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.RecipeRegistry;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.crafting.SmeltingRecipe;
import dev.brmz.sapientia.api.crafting.VanillaRecipe;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.api.overrides.RecipeOverride;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
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
    private volatile int revision;
    private final Map<NamespacedKey, VanillaRecipe> vanillaRecipes = new LinkedHashMap<>();
    private final List<NamespacedKey> installedVanilla = new ArrayList<>();
    private final Map<NamespacedKey, SmeltingRecipe> smeltingRecipes = new LinkedHashMap<>();
    private final List<NamespacedKey> installedSmelting = new ArrayList<>();

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
        revision++;
    }

    @Override
    public void registerVanilla(@NotNull VanillaRecipe recipe) {
        if (vanillaRecipes.putIfAbsent(recipe.id(), recipe) != null) {
            throw new IllegalStateException("Duplicate vanilla recipe id: " + recipe.id());
        }
    }

    @Override
    public void registerSmelting(@NotNull SmeltingRecipe recipe) {
        if (smeltingRecipes.putIfAbsent(recipe.id(), recipe) != null) {
            throw new IllegalStateException("Duplicate smelting recipe id: " + recipe.id());
        }
    }

    @Override
    public @NotNull Collection<SmeltingRecipe> smeltingRecipes() {
        return Collections.unmodifiableCollection(new ArrayList<>(smeltingRecipes.values()));
    }

    /** The furnace recipe with this key, or {@code null} when it is not a Sapientia one. */
    public @Nullable SmeltingRecipe smeltingRecipe(@NotNull NamespacedKey key) {
        return smeltingRecipes.get(key);
    }

    /** The furnace recipe that makes {@code item}, if any. */
    public @Nullable SmeltingRecipe smeltingRecipeFor(@NotNull NamespacedKey item) {
        for (SmeltingRecipe recipe : smeltingRecipes.values()) {
            if (recipe.result().equals(item)) return recipe;
        }
        return null;
    }

    /**
     * Adds the furnace recipes to the server: furnace recipes for vanilla
     * furnaces, and the recipe type of the block's vanilla furnace for recipes
     * of a Sapientia furnace. Inputs match the exact Sapientia stack.
     *
     * @param baseOf base material of a Sapientia block, or {@code null} if unknown
     */
    public void installSmelting(@NotNull Logger logger,
                                @NotNull java.util.function.Function<NamespacedKey, org.bukkit.Material> baseOf) {
        for (SmeltingRecipe recipe : smeltingRecipes.values()) {
            ItemStack input = itemRegistry.createStack(recipe.input().toString(), 1);
            ItemStack result = itemRegistry.createStack(recipe.result().toString(), recipe.amount());
            org.bukkit.Material base = recipe.furnace() == null ? org.bukkit.Material.FURNACE : baseOf.apply(recipe.furnace());
            if (input == null || result == null || base == null) {
                logger.warning("Smelting recipe " + recipe.id() + " refers to an unknown item or furnace.");
                continue;
            }
            RecipeChoice choice = RecipeChoice.exactChoice(input);
            Recipe bukkit = switch (base) {
                case BLAST_FURNACE -> new BlastingRecipe(recipe.id(), result, choice, recipe.experience(), recipe.cookTicks());
                case SMOKER -> new SmokingRecipe(recipe.id(), result, choice, recipe.experience(), recipe.cookTicks());
                case FURNACE -> new FurnaceRecipe(recipe.id(), result, choice, recipe.experience(), recipe.cookTicks());
                default -> null;
            };
            if (bukkit == null) {
                logger.warning("Smelting recipe " + recipe.id() + ": " + recipe.furnace() + " is not a furnace block.");
                continue;
            }
            Bukkit.removeRecipe(recipe.id());
            if (Bukkit.addRecipe(bukkit)) {
                installedSmelting.add(recipe.id());
            }
        }
        logger.info("Added " + installedSmelting.size() + " Sapientia furnace recipe(s).");
    }

    /** Removes the furnace recipes added by {@link #installSmelting}. */
    public void uninstallSmelting() {
        for (NamespacedKey key : installedSmelting) {
            Bukkit.removeRecipe(key);
        }
        installedSmelting.clear();
    }

    @Override
    public @NotNull Collection<VanillaRecipe> vanillaRecipes() {
        return Collections.unmodifiableCollection(new ArrayList<>(vanillaRecipes.values()));
    }

    /** The vanilla crafting table recipe that makes {@code item}, if any. */
    public @Nullable VanillaRecipe vanillaRecipeFor(@NotNull NamespacedKey item) {
        for (VanillaRecipe recipe : vanillaRecipes.values()) {
            if (recipe.result().equals(item)) return recipe;
        }
        return null;
    }

    /**
     * Adds the registered vanilla recipes to the server, with Sapientia stacks as
     * results. Returns their keys, for discovering them in players' recipe books.
     */
    public @NotNull List<NamespacedKey> installVanilla(@NotNull Logger logger) {
        for (VanillaRecipe recipe : vanillaRecipes.values()) {
            ItemStack result = itemRegistry.createStack(recipe.result().toString(), recipe.amount());
            if (result == null) {
                logger.warning("Vanilla recipe " + recipe.id() + " makes unknown item " + recipe.result());
                continue;
            }
            Recipe bukkit;
            if (recipe.isShaped()) {
                ShapedRecipe shaped = new ShapedRecipe(recipe.id(), result);
                shaped.shape(recipe.shape().toArray(String[]::new));
                recipe.ingredients().forEach((key, materials) ->
                        shaped.setIngredient(key, new RecipeChoice.MaterialChoice(List.copyOf(materials))));
                bukkit = shaped;
            } else {
                ShapelessRecipe shapeless = new ShapelessRecipe(recipe.id(), result);
                recipe.ingredients().values().forEach(materials ->
                        shapeless.addIngredient(new RecipeChoice.MaterialChoice(List.copyOf(materials))));
                bukkit = shapeless;
            }
            Bukkit.removeRecipe(recipe.id());
            if (Bukkit.addRecipe(bukkit)) {
                installedVanilla.add(recipe.id());
            }
        }
        logger.info("Added " + installedVanilla.size() + " Sapientia recipe(s) to the vanilla crafting table.");
        return List.copyOf(installedVanilla);
    }

    /** Removes the vanilla recipes added by {@link #installVanilla}. */
    public void uninstallVanilla() {
        for (NamespacedKey key : installedVanilla) {
            Bukkit.removeRecipe(key);
        }
        installedVanilla.clear();
    }

    /** Increments whenever a recipe is registered, so indexes over recipes know when to rebuild. */
    public int revision() {
        return revision;
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
