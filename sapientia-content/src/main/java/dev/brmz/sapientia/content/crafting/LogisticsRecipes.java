package dev.brmz.sapientia.content.crafting;

import java.util.List;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.guide.GuideCategory;
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
 * 1.8.0 advanced-logistics recipes (T-441..T-443 / T-448).
 *
 * <p>Every new block has at least one workbench recipe so the player can craft
 * it without {@code /sapientia give}. The packaging blocks gate behind 1.6.0
 * HV components; the comparator / fluid sensor reuse vanilla redstone idioms.
 */
public final class LogisticsRecipes {

    private LogisticsRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Vanilla / shared ingredients.
        RecipeIngredient redstone   = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient hopper     = RecipeIngredient.of(Material.HOPPER);
        RecipeIngredient barrel     = RecipeIngredient.of(Material.BARREL);
        RecipeIngredient observer   = RecipeIngredient.of(Material.OBSERVER);
        RecipeIngredient comparator = RecipeIngredient.of(Material.COMPARATOR);
        RecipeIngredient dropper    = RecipeIngredient.of(Material.DROPPER);
        RecipeIngredient dispenser  = RecipeIngredient.of(Material.DISPENSER);
        RecipeIngredient piston     = RecipeIngredient.of(Material.PISTON);
        RecipeIngredient lever      = RecipeIngredient.of(Material.LEVER);
        RecipeIngredient rail       = RecipeIngredient.of(Material.RAIL);

        // Sapientia ingredients.
        RecipeIngredient itemCable   = RecipeIngredient.of(new NamespacedKey(plugin, "item_cable"));
        RecipeIngredient itemFilter  = RecipeIngredient.of(new NamespacedKey(plugin, "item_filter"));
        RecipeIngredient fluidPipe   = RecipeIngredient.of(new NamespacedKey(plugin, "fluid_pipe"));
        RecipeIngredient cableT2     = RecipeIngredient.of(new NamespacedKey(plugin, "cable_t2"));
        RecipeIngredient ironPlate   = ingot(plugin, Metal.STAINLESS_STEEL, MetalForm.PLATE);
        RecipeIngredient brassPlate  = ingot(plugin, Metal.BRASS,           MetalForm.PLATE);
        RecipeIngredient brassGear   = ingot(plugin, Metal.BRASS,           MetalForm.GEAR);
        RecipeIngredient circuitT1   = component(plugin, Component.CIRCUIT_T1);
        RecipeIngredient motorT1     = component(plugin, Component.MOTOR_T1);
        // Era 10 logistics has no circuit boards yet: repeaters do the control logic.
        RecipeIngredient repeater    = RecipeIngredient.of(Material.REPEATER);

        // ---------- Item logistics extras (T-441) -------------------------------------------

        // item_buffer — barrel + circuit + item-cable shell.
        register(api, key(plugin, "recipe_item_buffer"),
                List.of(itemCable,    barrel,        itemCable,
                        ironPlate,    repeater,      ironPlate,
                        itemCable,    barrel,        itemCable),
                stack(api, key(plugin, "item_buffer"), 1),
                GuideCategory.LOGISTICS);

        // item_splitter — observer-driven 1-in/3-out distributor.
        register(api, key(plugin, "recipe_item_splitter"),
                List.of(brassPlate,   itemCable,     brassPlate,
                        observer,     repeater,      observer,
                        brassPlate,   itemCable,     brassPlate),
                stack(api, key(plugin, "item_splitter"), 1),
                GuideCategory.LOGISTICS);

        // filter_chamber — multi-pass filter.
        register(api, key(plugin, "recipe_filter_chamber"),
                List.of(ironPlate,    itemFilter,    ironPlate,
                        itemFilter,   comparator,    itemFilter,
                        ironPlate,    itemFilter,    ironPlate),
                stack(api, key(plugin, "filter_chamber"), 1),
                GuideCategory.LOGISTICS);

        // overflow_module — hopper + low-tier circuit.
        register(api, key(plugin, "recipe_overflow_module"),
                List.of(ironPlate,    hopper,        ironPlate,
                        hopper,       repeater,      hopper,
                        ironPlate,    redstone,      ironPlate),
                stack(api, key(plugin, "overflow_module"), 1),
                GuideCategory.LOGISTICS);

        // comparator_sensor — vanilla comparator + sapientia circuit.
        register(api, key(plugin, "recipe_comparator_sensor"),
                List.of(brassPlate,   comparator,    brassPlate,
                        redstone,     repeater,      redstone,
                        brassPlate,   comparator,    brassPlate),
                stack(api, key(plugin, "comparator_sensor"), 2),
                GuideCategory.LOGISTICS);

        // packager — dropper + circuit + motor.
        register(api, key(plugin, "recipe_packager"),
                List.of(brassPlate,   motorT1,       brassPlate,
                        dropper,      circuitT1,     dropper,
                        brassPlate,   piston,        brassPlate),
                stack(api, key(plugin, "packager"), 1),
                GuideCategory.LOGISTICS);

        // unpackager — dispenser + circuit + motor (mirror of packager).
        register(api, key(plugin, "recipe_unpackager"),
                List.of(brassPlate,   piston,        brassPlate,
                        dispenser,    circuitT1,     dispenser,
                        brassPlate,   motorT1,       brassPlate),
                stack(api, key(plugin, "unpackager"), 1),
                GuideCategory.LOGISTICS);

        // ---------- Conveyor (T-442) ---------------------------------------------------------

        // conveyor_belt — rail + brass gear + low-tier wiring; cheap stack of 4.
        register(api, key(plugin, "recipe_conveyor_belt"),
                List.of(brassPlate,   rail,          brassPlate,
                        brassGear,    cableT2,       brassGear,
                        brassPlate,   rail,          brassPlate),
                stack(api, key(plugin, "conveyor_belt"), 4),
                GuideCategory.LOGISTICS);

        // ---------- Fluid logistics extras (T-443) -------------------------------------------

        // fluid_valve — pipe + lever; produces a stack of 2.
        register(api, key(plugin, "recipe_fluid_valve"),
                List.of(ironPlate,    fluidPipe,     ironPlate,
                        lever,        repeater,      lever,
                        ironPlate,    fluidPipe,     ironPlate),
                stack(api, key(plugin, "fluid_valve"), 2),
                GuideCategory.LOGISTICS);

        // fluid_level_sensor — comparator + circuit + fluid pipe.
        register(api, key(plugin, "recipe_fluid_level_sensor"),
                List.of(ironPlate,    comparator,    ironPlate,
                        fluidPipe,    repeater,      fluidPipe,
                        ironPlate,    comparator,    ironPlate),
                stack(api, key(plugin, "fluid_level_sensor"), 1),
                GuideCategory.LOGISTICS);
    }

    // ---------- helpers ---------------------------------------------------------------------

    private static RecipeIngredient component(Plugin plugin, Component c) {
        return RecipeIngredient.of(new NamespacedKey(plugin, c.idBase()));
    }

    private static RecipeIngredient ingot(Plugin plugin, Metal metal, MetalForm form) {
        return RecipeIngredient.of(MetalCatalog.idOf(plugin, metal, form));
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
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
