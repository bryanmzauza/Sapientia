package dev.brmz.sapientia.core.progression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * The coherence rule of the eras: an item of era N is only made from
 * ingredients of eras 0 to N. Vanilla ingredients are not tracked and always
 * pass. Checked at start-up over every workbench and machine recipe.
 */
public final class EraCoherence {

    /** A recipe reduced to its Sapientia result and ingredients. */
    public record Recipe(@NotNull String id, @NotNull NamespacedKey result, @NotNull Collection<NamespacedKey> ingredients) {}

    /** An ingredient from a later era than the result it makes. */
    public record Violation(@NotNull String recipe, @NotNull NamespacedKey result, @NotNull Era resultEra,
                            @NotNull NamespacedKey ingredient, @NotNull Era ingredientEra) {
        @Override
        public @NotNull String toString() {
            return recipe + ": " + result + " (era " + resultEra.number() + ") uses " + ingredient
                    + " (era " + ingredientEra.number() + ")";
        }
    }

    private EraCoherence() {}

    public static @NotNull List<Violation> check(@NotNull Collection<Recipe> recipes,
                                                 @NotNull Function<NamespacedKey, Era> eraOf) {
        List<Violation> out = new ArrayList<>();
        for (Recipe recipe : recipes) {
            Era resultEra = eraOf.apply(recipe.result());
            for (NamespacedKey ingredient : recipe.ingredients()) {
                Era ingredientEra = eraOf.apply(ingredient);
                if (ingredientEra.isAfter(resultEra)) {
                    out.add(new Violation(recipe.id(), recipe.result(), resultEra, ingredient, ingredientEra));
                }
            }
        }
        return out;
    }
}
