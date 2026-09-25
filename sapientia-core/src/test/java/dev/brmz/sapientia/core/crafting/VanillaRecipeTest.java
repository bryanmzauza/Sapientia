package dev.brmz.sapientia.core.crafting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.brmz.sapientia.api.crafting.VanillaRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class VanillaRecipeTest {

    private static final NamespacedKey ID = new NamespacedKey("sapientia", "vanilla_workbench");
    private static final NamespacedKey RESULT = new NamespacedKey("sapientia", "workbench");

    @Test
    void shapedRecipesNeedAnIngredientForEveryCharacter() {
        VanillaRecipe recipe = VanillaRecipe.shaped(ID, RESULT, 1, List.of("CPC", "FTF", "CPC"), Map.of(
                'C', Set.of(Material.COBBLESTONE), 'P', Set.of(Material.OAK_PLANKS),
                'F', Set.of(Material.FLINT), 'T', Set.of(Material.CRAFTING_TABLE)));
        assertThat(recipe.isShaped()).isTrue();

        assertThatThrownBy(() -> VanillaRecipe.shaped(ID, RESULT, 1, List.of("CXC"),
                Map.of('C', Set.of(Material.COBBLESTONE))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VanillaRecipe.shaped(ID, RESULT, 1, List.of("CCCC"),
                Map.of('C', Set.of(Material.COBBLESTONE))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shapelessRecipesUseEachIngredientOnce() {
        VanillaRecipe recipe = VanillaRecipe.shapeless(ID, RESULT, 1, Set.of(Material.BOOK), Set.of(Material.FLINT));

        assertThat(recipe.isShaped()).isFalse();
        assertThat(recipe.ingredients()).hasSize(2);
        assertThatThrownBy(() -> VanillaRecipe.shapeless(ID, RESULT, 0, Set.of(Material.BOOK)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
