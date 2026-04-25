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
 * 1.7.0 geo &amp; atmosphere recipes (T-431..T-436 / T-439).
 *
 * <p>Each new block has at least one workbench recipe so the player can craft it
 * without {@code /sapientia give}. The HV-tier multiblock controllers gate
 * behind 1.6.0 HV components (circuit T3, RAM T3); the GPS infra reuses gold
 * + redstone + ender pearl per the established "transmit/route" idiom.
 */
public final class GeoRecipes {

    private GeoRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Vanilla / shared ingredients.
        RecipeIngredient gold       = RecipeIngredient.of(Material.GOLD_INGOT);
        RecipeIngredient redstone   = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient diamond    = RecipeIngredient.of(Material.DIAMOND);
        RecipeIngredient ender      = RecipeIngredient.of(Material.ENDER_PEARL);
        RecipeIngredient amethyst   = RecipeIngredient.of(Material.AMETHYST_SHARD);
        RecipeIngredient piston     = new RecipeIngredient.Vanilla(Material.PISTON, 1);
        RecipeIngredient compass    = RecipeIngredient.of(Material.COMPASS);
        RecipeIngredient spyglass   = RecipeIngredient.of(Material.SPYGLASS);
        RecipeIngredient paper      = RecipeIngredient.of(Material.PAPER);

        // Sapientia ingredients.
        RecipeIngredient mvCasing       = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing_mv"));
        RecipeIngredient stainlessCasing = RecipeIngredient.of(new NamespacedKey(plugin, "stainless_steel_casing"));
        RecipeIngredient cableT3        = RecipeIngredient.of(new NamespacedKey(plugin, "cable_t3"));
        RecipeIngredient stainlessPlate = ingot(plugin, Metal.STAINLESS_STEEL, MetalForm.PLATE);
        RecipeIngredient damascusPlate  = ingot(plugin, Metal.DAMASCUS_STEEL,  MetalForm.PLATE);
        RecipeIngredient titaniumPlate  = ingot(plugin, Metal.TITANIUM,        MetalForm.PLATE);
        RecipeIngredient nichromeWire   = ingot(plugin, Metal.NICHROME,        MetalForm.WIRE);
        RecipeIngredient siliconWafer   = component(plugin, Component.SILICON_WAFER);
        RecipeIngredient circuitT2      = component(plugin, Component.CIRCUIT_T2);
        RecipeIngredient circuitT3      = component(plugin, Component.CIRCUIT_T3);
        RecipeIngredient motorT2        = component(plugin, Component.MOTOR_T2);
        RecipeIngredient motorT3        = component(plugin, Component.MOTOR_T3);
        RecipeIngredient ramT3          = component(plugin, Component.RAM_T3);

        // ---------- Multiblock controllers (T-431 / T-432 / T-434) ---------------------------

        // quarry_controller — heavy MV-tier industrial assembly.
        register(api, key(plugin, "recipe_quarry_controller"),
                List.of(damascusPlate, motorT3,        damascusPlate,
                        stainlessCasing, circuitT3,    stainlessCasing,
                        damascusPlate, piston,         damascusPlate),
                stack(api, key(plugin, "quarry_controller"), 1),
                GuideCategory.MACHINE);

        // drill_rig_controller — even heavier; sub-bedrock prospecting.
        register(api, key(plugin, "recipe_drill_rig_controller"),
                List.of(damascusPlate, ramT3,          damascusPlate,
                        stainlessCasing, circuitT3,    stainlessCasing,
                        damascusPlate, motorT3,        damascusPlate),
                stack(api, key(plugin, "drill_rig_controller"), 1),
                GuideCategory.MACHINE);

        // desalinator_controller — sea-water → fresh-water + rock-salt.
        register(api, key(plugin, "recipe_desalinator_controller"),
                List.of(stainlessPlate, nichromeWire,  stainlessPlate,
                        stainlessCasing, circuitT2,    stainlessCasing,
                        stainlessPlate, nichromeWire,  stainlessPlate),
                stack(api, key(plugin, "desalinator_controller"), 1),
                GuideCategory.MACHINE);

        // ---------- Machines (T-433) ---------------------------------------------------------

        register(api, key(plugin, "recipe_gas_extractor"),
                List.of(titaniumPlate, motorT2,        titaniumPlate,
                        cableT3,       mvCasing,       cableT3,
                        titaniumPlate, piston,         titaniumPlate),
                stack(api, key(plugin, "gas_extractor"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_atmospheric_collector"),
                List.of(stainlessPlate, motorT2,       stainlessPlate,
                        siliconWafer,   mvCasing,      siliconWafer,
                        stainlessPlate, circuitT2,     stainlessPlate),
                stack(api, key(plugin, "atmospheric_collector"), 1),
                GuideCategory.MACHINE);

        // ---------- GPS infra (T-436) --------------------------------------------------------

        register(api, key(plugin, "recipe_gps_transmitter"),
                List.of(gold,         ender,          gold,
                        redstone,     circuitT3,      redstone,
                        gold,         diamond,        gold),
                stack(api, key(plugin, "gps_transmitter"), 1),
                GuideCategory.LOGISTICS);

        register(api, key(plugin, "recipe_gps_marker"),
                List.of(gold,         redstone,       gold,
                        redstone,     ender,          redstone,
                        gold,         redstone,       gold),
                stack(api, key(plugin, "gps_marker"), 4),
                GuideCategory.LOGISTICS);

        register(api, key(plugin, "recipe_gps_handheld_map"),
                List.of(paper,        compass,        paper,
                        circuitT2,    ender,          circuitT2,
                        paper,        amethyst,       paper),
                stack(api, key(plugin, "gps_handheld_map"), 1),
                GuideCategory.TOOL);

        register(api, key(plugin, "recipe_prospector"),
                List.of(amethyst,     spyglass,       amethyst,
                        circuitT2,    ender,          circuitT2,
                        titaniumPlate, redstone,      titaniumPlate),
                stack(api, key(plugin, "prospector"), 1),
                GuideCategory.TOOL);
    }

    // ---------- helpers ----------------------------------------------------------------------

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
