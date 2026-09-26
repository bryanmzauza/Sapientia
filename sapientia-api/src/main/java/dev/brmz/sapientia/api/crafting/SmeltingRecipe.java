package dev.brmz.sapientia.api.crafting;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Sapientia item smelted into another one in a furnace.
 *
 * <p>With {@code furnace == null} the recipe runs in any vanilla furnace.
 * Otherwise it only runs in that Sapientia block, which must stand on a vanilla
 * furnace, blast furnace or smoker (for example the clay furnace); other
 * furnaces of that kind refuse it.
 *
 * @param id         recipe key
 * @param input      Sapientia item smelted
 * @param result     Sapientia item produced
 * @param amount     result amount
 * @param cookTicks  smelting time in ticks
 * @param experience experience given per result
 * @param furnace    Sapientia furnace block required, or {@code null} for vanilla furnaces
 */
public record SmeltingRecipe(
        @NotNull NamespacedKey id,
        @NotNull NamespacedKey input,
        @NotNull NamespacedKey result,
        int amount,
        int cookTicks,
        float experience,
        @Nullable NamespacedKey furnace) {

    public SmeltingRecipe {
        if (amount < 1) throw new IllegalArgumentException("amount must be >= 1");
        if (cookTicks < 1) throw new IllegalArgumentException("cookTicks must be >= 1");
        if (experience < 0) throw new IllegalArgumentException("experience must be >= 0");
    }

    /** A recipe for any vanilla furnace. */
    public static @NotNull SmeltingRecipe furnace(@NotNull NamespacedKey id, @NotNull NamespacedKey input,
                                                  @NotNull NamespacedKey result, int cookTicks) {
        return new SmeltingRecipe(id, input, result, 1, cookTicks, 0.1f, null);
    }
}
