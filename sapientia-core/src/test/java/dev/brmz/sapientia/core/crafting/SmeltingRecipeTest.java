package dev.brmz.sapientia.core.crafting;

import dev.brmz.sapientia.api.crafting.SmeltingRecipe;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SmeltingRecipeTest {

    private static final NamespacedKey ID = new NamespacedKey("sapientia", "smelt_fire_brick");
    private static final NamespacedKey INPUT = new NamespacedKey("sapientia", "fire_clay");
    private static final NamespacedKey RESULT = new NamespacedKey("sapientia", "fire_brick");

    @Test
    void furnaceRecipesRunInAnyVanillaFurnace() {
        SmeltingRecipe recipe = SmeltingRecipe.furnace(ID, INPUT, RESULT, 200);

        assertThat(recipe.furnace()).isNull();
        assertThat(recipe.amount()).isEqualTo(1);
        assertThat(recipe.cookTicks()).isEqualTo(200);
    }

    @Test
    void rejectsEmptyResultsAndTimes() {
        assertThatThrownBy(() -> new SmeltingRecipe(ID, INPUT, RESULT, 0, 200, 0f, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SmeltingRecipe(ID, INPUT, RESULT, 1, 0, 0f, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SmeltingRecipe(ID, INPUT, RESULT, 1, 200, -1f, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
