package dev.brmz.sapientia.core.crafting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.core.crafting.SapientiaRecipeRegistry.MatchCell;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the 3&times;3 shape-exact matcher (T-131 / 0.4.0). Exercises
 * the pure {@link SapientiaRecipeRegistry#matchCells(MatchCell[])} path so no
 * Bukkit runtime is required.
 */
final class SapientiaRecipeRegistryTest {

    private static final NamespacedKey WRENCH_RECIPE =
            NamespacedKey.fromString("sapientia:recipe_wrench");
    private static final NamespacedKey CABLE_RECIPE =
            NamespacedKey.fromString("sapientia:recipe_cable");

    @Test
    void matchesExactShape() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        MatchCell[] cells = empty();
        cells[1] = iron();
        cells[3] = iron();
        cells[4] = iron();
        cells[7] = stick();
        assertThat(reg.matchCells(cells)).isPresent();
    }

    @Test
    void rejectsWhenIngredientMissing() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        MatchCell[] cells = empty();
        cells[1] = iron();
        cells[3] = iron();
        // slot 4 missing
        cells[7] = stick();
        assertThat(reg.matchCells(cells)).isEmpty();
    }

    @Test
    void rejectsTranslatedPattern() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        MatchCell[] cells = empty();
        cells[2] = iron();
        cells[4] = iron();
        cells[5] = iron();
        cells[8] = stick();
        assertThat(reg.matchCells(cells)).isEmpty();
    }

    @Test
    void rejectsExtraItemsInEmptyCell() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        MatchCell[] cells = empty();
        cells[0] = new MatchCell(Material.DIRT, null, 1);
        cells[1] = iron();
        cells[3] = iron();
        cells[4] = iron();
        cells[7] = stick();
        assertThat(reg.matchCells(cells)).isEmpty();
    }

    @Test
    void rejectsSapientiaTaggedStackForVanillaCell() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        MatchCell[] cells = empty();
        cells[1] = iron();
        cells[3] = iron();
        cells[4] = new MatchCell(Material.IRON_INGOT, "sapientia:fake", 1); // tagged
        cells[7] = stick();
        assertThat(reg.matchCells(cells)).isEmpty();
    }

    @Test
    void pickFirstRegisteredOnOverlap() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        reg.register(cableRecipe());
        MatchCell[] cells = empty();
        cells[1] = iron();
        cells[3] = iron();
        cells[4] = iron();
        cells[7] = stick();
        SapientiaRecipe picked = reg.matchCells(cells).orElseThrow();
        assertThat(picked.id()).isEqualTo(WRENCH_RECIPE);
    }

    @Test
    void rejectsDuplicateIds() {
        SapientiaRecipeRegistry reg = new SapientiaRecipeRegistry(null);
        reg.register(wrenchRecipe());
        try {
            reg.register(wrenchRecipe());
            org.junit.jupiter.api.Assertions.fail("expected duplicate id to throw");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    // ---------------------------------------------------------------- fixtures

    private static SapientiaRecipe wrenchRecipe() {
        return fakeRecipe(WRENCH_RECIPE, List.of(
                RecipeIngredient.empty(), RecipeIngredient.of(Material.IRON_INGOT), RecipeIngredient.empty(),
                RecipeIngredient.of(Material.IRON_INGOT), RecipeIngredient.of(Material.IRON_INGOT), RecipeIngredient.empty(),
                RecipeIngredient.empty(), RecipeIngredient.of(Material.STICK), RecipeIngredient.empty()));
    }

    private static SapientiaRecipe cableRecipe() {
        return fakeRecipe(CABLE_RECIPE, List.of(
                RecipeIngredient.of(Material.IRON_INGOT), RecipeIngredient.of(Material.REDSTONE), RecipeIngredient.of(Material.IRON_INGOT),
                RecipeIngredient.empty(), RecipeIngredient.empty(), RecipeIngredient.empty(),
                RecipeIngredient.empty(), RecipeIngredient.empty(), RecipeIngredient.empty()));
    }

    private static SapientiaRecipe fakeRecipe(NamespacedKey id, List<RecipeIngredient> pattern) {
        return new SapientiaRecipe() {
            @Override public NamespacedKey id() { return id; }
            @Override public List<RecipeIngredient> pattern() { return pattern; }
            @Override public ItemStack result() { throw new UnsupportedOperationException("not needed"); }
            @Override public GuideCategory category() { return GuideCategory.MISC; }
        };
    }

    private static MatchCell iron()  { return new MatchCell(Material.IRON_INGOT, null, 1); }
    private static MatchCell stick() { return new MatchCell(Material.STICK, null, 1); }

    private static MatchCell[] empty() { return new MatchCell[9]; }
}
