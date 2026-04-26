package dev.brmz.sapientia.api.crafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * One cell of a 3&times;3 Sapientia crafting pattern (T-131 / 0.4.0). Three flavours:
 *
 * <ul>
 *   <li>{@link Vanilla} — matches a vanilla {@link Material} with {@code amount} required.</li>
 *   <li>{@link Sapientia} — matches a registered {@link dev.brmz.sapientia.api.item.SapientiaItem}
 *       by its {@link NamespacedKey} id with {@code amount} required.</li>
 *   <li>{@link Empty} — the slot must be empty.</li>
 * </ul>
 */
public sealed interface RecipeIngredient
        permits RecipeIngredient.Vanilla, RecipeIngredient.Sapientia, RecipeIngredient.Empty {

    int amount();

    /** Vanilla material cell. */
    record Vanilla(@NotNull Material material, int amount) implements RecipeIngredient {
        public Vanilla {
            if (material == null) throw new IllegalArgumentException("material must not be null");
            if (amount < 1) throw new IllegalArgumentException("amount must be >= 1");
        }
    }

    /** Sapientia-registered item cell. */
    record Sapientia(@NotNull NamespacedKey id, int amount) implements RecipeIngredient {
        public Sapientia {
            if (id == null) throw new IllegalArgumentException("id must not be null");
            if (amount < 1) throw new IllegalArgumentException("amount must be >= 1");
        }
    }

    /** Empty-cell marker (singleton via {@link #INSTANCE}). */
    final class Empty implements RecipeIngredient {
        public static final Empty INSTANCE = new Empty();
        private Empty() {}
        @Override public int amount() { return 0; }
    }

    static @NotNull RecipeIngredient empty() { return Empty.INSTANCE; }
    static @NotNull RecipeIngredient of(@NotNull Material material) { return new Vanilla(material, 1); }
    static @NotNull RecipeIngredient of(@NotNull Material material, int amount) { return new Vanilla(material, amount); }
    static @NotNull RecipeIngredient of(@NotNull NamespacedKey id) { return new Sapientia(id, 1); }
    static @NotNull RecipeIngredient of(@NotNull NamespacedKey id, int amount) { return new Sapientia(id, amount); }
}
