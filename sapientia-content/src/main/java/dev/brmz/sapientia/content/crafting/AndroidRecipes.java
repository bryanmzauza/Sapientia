package dev.brmz.sapientia.content.crafting;

import java.util.List;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.android.AndroidType;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.android.AndroidUpgradeCatalog;
import dev.brmz.sapientia.content.android.AndroidUpgradeItem;
import dev.brmz.sapientia.content.electronics.Component;
import dev.brmz.sapientia.content.metallurgy.Metal;
import dev.brmz.sapientia.content.metallurgy.MetalCatalog;
import dev.brmz.sapientia.content.metallurgy.MetalForm;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 1.9.0 android catalogue recipes (T-451 / T-452 / T-454 / T-460).
 *
 * <p>8 android shells + 16 upgrade items, all crafted on the
 * {@code sapientia_workbench}. Every android shell sinks a HV-tier
 * processor + a metal frame (4 stainless plates + 2 stainless ingots) and
 * a kind-specific tool head, anchoring them firmly behind the 1.6.0
 * electronics tier.
 */
public final class AndroidRecipes {

    private AndroidRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Vanilla / shared ingredients.
        RecipeIngredient redstone   = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient bone       = RecipeIngredient.of(Material.BONE);
        RecipeIngredient slime      = RecipeIngredient.of(Material.SLIME_BALL);
        RecipeIngredient leather    = RecipeIngredient.of(Material.LEATHER);

        // Sapientia ingredients.
        RecipeIngredient processorT3 = component(plugin, Component.PROCESSOR_T3);
        RecipeIngredient processorT2 = component(plugin, Component.PROCESSOR_T2);
        RecipeIngredient processorT1 = component(plugin, Component.PROCESSOR_T1);
        RecipeIngredient circuitT3   = component(plugin, Component.CIRCUIT_T3);
        RecipeIngredient circuitT2   = component(plugin, Component.CIRCUIT_T2);
        RecipeIngredient circuitT1   = component(plugin, Component.CIRCUIT_T1);
        RecipeIngredient ramT3       = component(plugin, Component.RAM_T3);
        RecipeIngredient motorT3     = component(plugin, Component.MOTOR_T3);
        RecipeIngredient motorT2     = component(plugin, Component.MOTOR_T2);
        RecipeIngredient motorT1     = component(plugin, Component.MOTOR_T1);
        RecipeIngredient coilT3      = component(plugin, Component.COIL_T3);
        RecipeIngredient stainlessPlate = ingot(plugin, Metal.STAINLESS_STEEL, MetalForm.PLATE);
        RecipeIngredient stainlessIngot = ingot(plugin, Metal.STAINLESS_STEEL, MetalForm.INGOT);

        // ---------- Android shells (T-452) ---------------------------------------------------
        // Generic chassis layout:
        //   plate plate plate
        //   ingot brain ingot     ← brain = processor_t3 (slayer/builder/trader use ramT3)
        //   plate  tool plate
        // The "tool" slot makes each archetype unique without bloating the
        // recipe count beyond reason. Numbers below are deliberately
        // expensive so 4-android-per-chunk caps stay meaningful.

        register(api, key(plugin, "recipe_android_farmer"),
                List.of(stainlessPlate, stainlessPlate,                  stainlessPlate,
                        stainlessIngot, processorT3,                     stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.IRON_HOE), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.FARMER), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_lumberjack"),
                List.of(stainlessPlate, stainlessPlate,                  stainlessPlate,
                        stainlessIngot, processorT3,                     stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.IRON_AXE), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.LUMBERJACK), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_miner"),
                List.of(stainlessPlate, stainlessPlate,                     stainlessPlate,
                        stainlessIngot, processorT3,                        stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.DIAMOND_PICKAXE), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.MINER), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_fisherman"),
                List.of(stainlessPlate, stainlessPlate,                       stainlessPlate,
                        stainlessIngot, processorT3,                          stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.FISHING_ROD), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.FISHERMAN), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_butcher"),
                List.of(stainlessPlate, stainlessPlate,                  stainlessPlate,
                        stainlessIngot, processorT3,                     stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.IRON_SWORD), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.BUTCHER), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_builder"),
                List.of(stainlessPlate, stainlessPlate,                       stainlessPlate,
                        stainlessIngot, ramT3,                                stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.SCAFFOLDING), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.BUILDER), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_slayer"),
                List.of(stainlessPlate, stainlessPlate,                          stainlessPlate,
                        stainlessIngot, processorT3,                             stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.NETHERITE_SWORD), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.SLAYER), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_android_trader"),
                List.of(stainlessPlate, stainlessPlate,                  stainlessPlate,
                        stainlessIngot, ramT3,                           stainlessIngot,
                        stainlessPlate, RecipeIngredient.of(Material.EMERALD), stainlessPlate),
                stack(api, androidKey(plugin, AndroidType.TRADER), 1),
                GuideCategory.MACHINE);

        // ---------- AI chip upgrades (T-454) -------------------------------------------------
        // Pattern: tier-1..4 circuit at the center, redstone perimeter.
        registerUpgrade(api, plugin, AndroidUpgradeItem.AI_CHIP_T1, circuitT1, redstone);
        registerUpgrade(api, plugin, AndroidUpgradeItem.AI_CHIP_T2, circuitT2, redstone);
        registerUpgrade(api, plugin, AndroidUpgradeItem.AI_CHIP_T3, circuitT3, redstone);
        registerUpgrade(api, plugin, AndroidUpgradeItem.AI_CHIP_T4, processorT3, redstone);

        // ---------- Motor chip upgrades (T-454) ----------------------------------------------
        // Pattern: tier motor at center, slime ball perimeter.
        registerUpgrade(api, plugin, AndroidUpgradeItem.MOTOR_CHIP_T1, motorT1, slime);
        registerUpgrade(api, plugin, AndroidUpgradeItem.MOTOR_CHIP_T2, motorT2, slime);
        registerUpgrade(api, plugin, AndroidUpgradeItem.MOTOR_CHIP_T3, motorT3, slime);
        registerUpgrade(api, plugin, AndroidUpgradeItem.MOTOR_CHIP_T4, motorT3, coilT3);

        // ---------- Armour plate upgrades (T-454) --------------------------------------------
        // Pattern: stainless plate at center, leather/bone perimeter.
        registerUpgrade(api, plugin, AndroidUpgradeItem.ARMOUR_PLATE_T1, stainlessPlate, leather);
        registerUpgrade(api, plugin, AndroidUpgradeItem.ARMOUR_PLATE_T2, stainlessPlate, bone);
        registerUpgrade(api, plugin, AndroidUpgradeItem.ARMOUR_PLATE_T3, stainlessPlate, stainlessIngot);
        registerUpgrade(api, plugin, AndroidUpgradeItem.ARMOUR_PLATE_T4, processorT2,    stainlessIngot);

        // ---------- Fuel module upgrades (T-454) ---------------------------------------------
        // Pattern: tier-specific fuel + circuit.
        registerUpgrade(api, plugin, AndroidUpgradeItem.FUEL_MODULE_T1, RecipeIngredient.of(Material.COAL),         circuitT1);
        registerUpgrade(api, plugin, AndroidUpgradeItem.FUEL_MODULE_T2, RecipeIngredient.of(Material.CHARCOAL),     circuitT2);
        registerUpgrade(api, plugin, AndroidUpgradeItem.FUEL_MODULE_T3, RecipeIngredient.of(Material.BLAZE_POWDER), circuitT3);
        registerUpgrade(api, plugin, AndroidUpgradeItem.FUEL_MODULE_T4, RecipeIngredient.of(Material.NETHER_STAR),  processorT3);
    }

    // ---------- helpers --------------------------------------------------------------------

    private static void registerUpgrade(@NotNull SapientiaAPI api,
                                        @NotNull Plugin plugin,
                                        @NotNull AndroidUpgradeItem upgrade,
                                        @NotNull RecipeIngredient core,
                                        @NotNull RecipeIngredient outer) {
        // Standard tier-upgrade pattern:
        //   outer  outer  outer
        //   outer   core  outer
        //   outer  outer  outer
        register(api, key(plugin, "recipe_" + upgrade.idBase()),
                List.of(outer, outer, outer,
                        outer, core,  outer,
                        outer, outer, outer),
                stack(api, AndroidUpgradeCatalog.idOf(plugin, upgrade), 1),
                GuideCategory.MATERIAL);
    }

    private static RecipeIngredient component(Plugin plugin, Component c) {
        return RecipeIngredient.of(new NamespacedKey(plugin, c.idBase()));
    }

    private static RecipeIngredient ingot(Plugin plugin, Metal metal, MetalForm form) {
        return RecipeIngredient.of(MetalCatalog.idOf(plugin, metal, form));
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    private static NamespacedKey androidKey(Plugin plugin, AndroidType type) {
        return new NamespacedKey(plugin, type.idBase());
    }

    private static ItemStack stack(SapientiaAPI api, NamespacedKey itemId, int amount) {
        return api.createStack(itemId, amount).orElseThrow(() ->
                new IllegalStateException("Sapientia item not registered: " + itemId));
    }

    private static void register(SapientiaAPI api, NamespacedKey id, List<RecipeIngredient> pattern,
                                 ItemStack result, GuideCategory category) {
        api.recipes().register(new BundledRecipe(id, pattern, result, category));
    }
}
