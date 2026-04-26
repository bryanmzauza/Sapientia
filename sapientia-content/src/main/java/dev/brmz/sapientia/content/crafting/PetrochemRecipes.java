package dev.brmz.sapientia.content.crafting;

import java.util.List;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.metallurgy.Metal;
import dev.brmz.sapientia.content.metallurgy.MetalCatalog;
import dev.brmz.sapientia.content.metallurgy.MetalForm;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 1.5.0 petrochemistry recipes (T-411..T-417). Covers crafting of:
 *
 * <ul>
 *   <li>{@code stainless_steel_casing} (8× steel proxy + 1× MV casing)</li>
 *   <li>{@code pumpjack} (MV casing + iron rods)</li>
 *   <li>{@code combustion_gen}, {@code biogas_gen} (LV/MV casings + furnace)</li>
 *   <li>{@code cracker}, {@code fermenter}, {@code still}, {@code bioreactor}</li>
 *   <li>{@code oil_refinery_controller} (8× stainless casing + 1× MV casing)</li>
 * </ul>
 */
public final class PetrochemRecipes {

    private PetrochemRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        RecipeIngredient iron = RecipeIngredient.of(Material.IRON_INGOT);
        RecipeIngredient ironBlock = new RecipeIngredient.Vanilla(Material.IRON_BLOCK, 1);
        RecipeIngredient redstone = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient blaze = RecipeIngredient.of(Material.BLAZE_POWDER);
        RecipeIngredient mvCasing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing_mv"));
        RecipeIngredient lvCasing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing"));
        RecipeIngredient stainless = RecipeIngredient.of(new NamespacedKey(plugin, "stainless_steel_casing"));
        RecipeIngredient nickelPlate = RecipeIngredient.of(idOf(plugin, Metal.NICKEL, MetalForm.PLATE));
        RecipeIngredient bronzePlate = RecipeIngredient.of(idOf(plugin, Metal.BRONZE, MetalForm.PLATE));
        RecipeIngredient brassPlate = RecipeIngredient.of(idOf(plugin, Metal.BRASS, MetalForm.PLATE));
        RecipeIngredient copperWire = RecipeIngredient.of(idOf(plugin, Metal.COPPER, MetalForm.WIRE));
        RecipeIngredient bucket = new RecipeIngredient.Vanilla(Material.BUCKET, 1);
        RecipeIngredient brewingStand = new RecipeIngredient.Vanilla(Material.BREWING_STAND, 1);
        RecipeIngredient composter = new RecipeIngredient.Vanilla(Material.COMPOSTER, 1);
        RecipeIngredient smoker = new RecipeIngredient.Vanilla(Material.SMOKER, 1);
        RecipeIngredient sculk = new RecipeIngredient.Vanilla(Material.SCULK_CATALYST, 1);
        RecipeIngredient soulCampfire = new RecipeIngredient.Vanilla(Material.SOUL_CAMPFIRE, 1);
        RecipeIngredient campfire = new RecipeIngredient.Vanilla(Material.CAMPFIRE, 1);
        RecipeIngredient piston = new RecipeIngredient.Vanilla(Material.PISTON, 1);

        // stainless_steel_casing: nickel plate ring around an MV casing → 8× casing
        register(api, new NamespacedKey(plugin, "recipe_stainless_steel_casing"),
                List.of(
                        nickelPlate, nickelPlate, nickelPlate,
                        nickelPlate, mvCasing,    nickelPlate,
                        nickelPlate, nickelPlate, nickelPlate),
                sapientiaStack(api, new NamespacedKey(plugin, "stainless_steel_casing"), 8),
                GuideCategory.MATERIAL);

        // pumpjack (MV consumer): iron block on top, MV casing core, piston bottom
        register(api, new NamespacedKey(plugin, "recipe_pumpjack"),
                List.of(
                        iron,     ironBlock, iron,
                        redstone, mvCasing,  redstone,
                        iron,     piston,    iron),
                sapientiaStack(api, new NamespacedKey(plugin, "pumpjack"), 1),
                GuideCategory.MACHINE);

        // combustion_gen (MV generator)
        register(api, new NamespacedKey(plugin, "recipe_combustion_gen"),
                List.of(
                        bronzePlate, soulCampfire, bronzePlate,
                        redstone,    mvCasing,     redstone,
                        bronzePlate, bucket,       bronzePlate),
                sapientiaStack(api, new NamespacedKey(plugin, "combustion_gen"), 1),
                GuideCategory.ENERGY);

        // biogas_gen (LV generator)
        register(api, new NamespacedKey(plugin, "recipe_biogas_gen"),
                List.of(
                        iron,     campfire, iron,
                        redstone, lvCasing, redstone,
                        iron,     bucket,   iron),
                sapientiaStack(api, new NamespacedKey(plugin, "biogas_gen"), 1),
                GuideCategory.ENERGY);

        // cracker (MV)
        register(api, new NamespacedKey(plugin, "recipe_cracker"),
                List.of(
                        brassPlate, brewingStand, brassPlate,
                        redstone,   mvCasing,     redstone,
                        copperWire, blaze,        copperWire),
                sapientiaStack(api, new NamespacedKey(plugin, "cracker"), 1),
                GuideCategory.MACHINE);

        // fermenter (MV)
        register(api, new NamespacedKey(plugin, "recipe_fermenter"),
                List.of(
                        brassPlate, composter, brassPlate,
                        redstone,   mvCasing,  redstone,
                        copperWire, bucket,    copperWire),
                sapientiaStack(api, new NamespacedKey(plugin, "fermenter"), 1),
                GuideCategory.MACHINE);

        // still (MV)
        register(api, new NamespacedKey(plugin, "recipe_still"),
                List.of(
                        bronzePlate, smoker,   bronzePlate,
                        redstone,    mvCasing, redstone,
                        copperWire,  bucket,   copperWire),
                sapientiaStack(api, new NamespacedKey(plugin, "still"), 1),
                GuideCategory.MACHINE);

        // bioreactor (LV)
        register(api, new NamespacedKey(plugin, "recipe_bioreactor"),
                List.of(
                        iron,       sculk,    iron,
                        redstone,   lvCasing, redstone,
                        copperWire, bucket,   copperWire),
                sapientiaStack(api, new NamespacedKey(plugin, "bioreactor"), 1),
                GuideCategory.MACHINE);

        // oil_refinery_controller: stainless ring around MV casing
        register(api, new NamespacedKey(plugin, "recipe_oil_refinery_controller"),
                List.of(
                        stainless, stainless, stainless,
                        stainless, mvCasing,  stainless,
                        stainless, stainless, stainless),
                sapientiaStack(api, new NamespacedKey(plugin, "oil_refinery_controller"), 1),
                GuideCategory.MACHINE);
    }

    private static NamespacedKey idOf(Plugin plugin, Metal metal, MetalForm form) {
        return MetalCatalog.idOf(plugin, metal, form);
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
}
