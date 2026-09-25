package dev.brmz.sapientia.api.crafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A recipe for the vanilla crafting table whose result is a Sapientia item.
 * Used for the few items made before the Sapientia Workbench exists (the guide
 * and the workbench itself). Each ingredient accepts any of its materials.
 *
 * <p>Shaped recipes have up to three rows of up to three characters, each
 * character a key of {@link #ingredients()} (a space is an empty cell);
 * shapeless recipes have no rows and use each ingredient once.
 *
 * @param id          recipe id, also used as the vanilla recipe key
 * @param result      Sapientia item produced
 * @param amount      stack size produced
 * @param shape       rows of a shaped recipe; empty for a shapeless one
 * @param ingredients materials accepted for each character, in order
 */
public record VanillaRecipe(
        @NotNull NamespacedKey id,
        @NotNull NamespacedKey result,
        int amount,
        @NotNull List<String> shape,
        @NotNull Map<Character, Set<Material>> ingredients) {

    public VanillaRecipe {
        if (amount < 1) throw new IllegalArgumentException("amount must be >= 1");
        if (ingredients.isEmpty()) throw new IllegalArgumentException("a recipe needs ingredients");
        if (shape.size() > 3 || shape.stream().anyMatch(row -> row.isEmpty() || row.length() > 3)) {
            throw new IllegalArgumentException("shape must have 1 to 3 rows of 1 to 3 characters");
        }
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && !ingredients.containsKey(c)) {
                    throw new IllegalArgumentException("shape uses '" + c + "' without an ingredient");
                }
            }
        }
        for (Set<Material> choice : ingredients.values()) {
            if (choice.isEmpty()) throw new IllegalArgumentException("an ingredient needs materials");
        }
        shape = List.copyOf(shape);
        ingredients = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    }

    public boolean isShaped() {
        return !shape.isEmpty();
    }

    /** A shaped recipe, e.g. {@code shaped(id, result, 1, List.of("CPC", "FTF", "CPC"), keys)}. */
    public static @NotNull VanillaRecipe shaped(@NotNull NamespacedKey id, @NotNull NamespacedKey result, int amount,
                                                @NotNull List<String> shape,
                                                @NotNull Map<Character, Set<Material>> ingredients) {
        return new VanillaRecipe(id, result, amount, shape, ingredients);
    }

    /** A shapeless recipe using each of the given ingredients once. */
    @SafeVarargs
    public static @NotNull VanillaRecipe shapeless(@NotNull NamespacedKey id, @NotNull NamespacedKey result, int amount,
                                                   @NotNull Set<Material>... ingredients) {
        Map<Character, Set<Material>> keys = new LinkedHashMap<>();
        char c = 'A';
        for (Set<Material> ingredient : ingredients) {
            keys.put(c++, ingredient);
        }
        return new VanillaRecipe(id, result, amount, List.of(), keys);
    }
}
