package dev.brmz.sapientia.content.crafting;

import java.util.List;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Three reference recipes bundled with Sapientia (T-131 / 0.4.0):
 * <ul>
 *   <li>{@code recipe/wrench} — 2 iron ingots + 1 stick in a small hammer shape,
 *       yielding the {@code sapientia:wrench} tool.</li>
 *   <li>{@code recipe/cable} — iron ingot / redstone / iron ingot across the top
 *       row, yielding 4× {@code sapientia:cable}.</li>
 *   <li>{@code recipe/generator} — iron frame around a furnace core with a
 *       redstone block fuel pad, yielding 1× {@code sapientia:generator}.</li>
 * </ul>
 */
public final class BundledRecipes {

    private BundledRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        register(api,
                new NamespacedKey(plugin, "recipe_wrench"),
                List.of(
                        empty(), iron(), empty(),
                        iron(),  iron(), empty(),
                        empty(), stick(), empty()),
                sapientiaStack(api, new NamespacedKey(plugin, "wrench"), 1),
                GuideCategory.TOOL);

        register(api,
                new NamespacedKey(plugin, "recipe_cable"),
                List.of(
                        iron(), redstone(), iron(),
                        empty(), empty(), empty(),
                        empty(), empty(), empty()),
                sapientiaStack(api, new NamespacedKey(plugin, "cable"), 4),
                GuideCategory.ENERGY);

        register(api,
                new NamespacedKey(plugin, "recipe_generator"),
                List.of(
                        iron(), iron(), iron(),
                        iron(), new RecipeIngredient.Vanilla(Material.FURNACE, 1), iron(),
                        iron(), new RecipeIngredient.Vanilla(Material.REDSTONE_BLOCK, 1), iron()),
                sapientiaStack(api, new NamespacedKey(plugin, "generator"), 1),
                GuideCategory.ENERGY);
    }

    private static void register(
            SapientiaAPI api,
            NamespacedKey id,
            List<RecipeIngredient> pattern,
            ItemStack result,
            GuideCategory category) {
        api.recipes().register(new BundledRecipe(id, pattern, result, category));
    }

    private static ItemStack sapientiaStack(SapientiaAPI api, NamespacedKey itemId, int amount) {
        return api.createStack(itemId, amount).orElseThrow(() ->
                new IllegalStateException("Sapientia item not registered yet: " + itemId));
    }

    private static RecipeIngredient iron()     { return RecipeIngredient.of(Material.IRON_INGOT); }
    private static RecipeIngredient stick()    { return RecipeIngredient.of(Material.STICK); }
    private static RecipeIngredient redstone() { return RecipeIngredient.of(Material.REDSTONE); }
    private static RecipeIngredient empty()    { return RecipeIngredient.empty(); }
}
