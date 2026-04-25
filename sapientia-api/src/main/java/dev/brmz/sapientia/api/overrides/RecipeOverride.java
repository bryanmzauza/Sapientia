package dev.brmz.sapientia.api.overrides;

import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Operator-authored tweak applied over a registered {@code SapientiaRecipe}
 * (T-160 / 0.5.0). Currently limited to output quantity; timing-related fields
 * will be added when the machine-bound crafting lands (1.2.0).
 */
public record RecipeOverride(
        @NotNull NamespacedKey id,
        @NotNull Optional<Integer> resultAmount) {

    public RecipeOverride {
        if (resultAmount.isPresent() && resultAmount.get() < 1) {
            throw new IllegalArgumentException("result_amount must be >= 1");
        }
    }
}
