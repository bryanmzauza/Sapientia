package dev.brmz.sapientia.api.machine;

import org.jetbrains.annotations.NotNull;

/**
 * Snapshot of the progress of a recipe currently being processed by a {@link Machine}.
 * See docs/api-spec.md §2.1.
 *
 * @param recipeId      stable identifier of the recipe, e.g. {@code "sapientia:iron_plate"}.
 * @param ticksElapsed  ticks completed so far (inclusive of the current one).
 * @param ticksRequired total ticks needed to finish the recipe.
 */
public record RecipeProgress(@NotNull String recipeId, int ticksElapsed, int ticksRequired) {

    public RecipeProgress {
        if (ticksElapsed < 0) {
            throw new IllegalArgumentException("ticksElapsed must be >= 0");
        }
        if (ticksRequired <= 0) {
            throw new IllegalArgumentException("ticksRequired must be > 0");
        }
        if (ticksElapsed > ticksRequired) {
            throw new IllegalArgumentException("ticksElapsed must be <= ticksRequired");
        }
    }

    /** Progress as a value in {@code [0.0, 1.0]}. */
    public double fraction() {
        return (double) ticksElapsed / (double) ticksRequired;
    }

    public boolean isComplete() {
        return ticksElapsed >= ticksRequired;
    }
}
