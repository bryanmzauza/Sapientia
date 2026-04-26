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
 * 1.6.0 electronics, HV-energy, gas-handling and HV-machine recipes
 * (T-422 / T-423 / T-425 / T-426 / T-428).
 *
 * <p>Recipe density is comparable to {@link MetallurgyRecipes} for the LV/MV
 * tiers — every new block has at least one workbench recipe so the player can
 * progress without {@code /sapientia give}. Component recipes use silicon and
 * the new HV alloys to gate progression behind 1.4.0 metallurgy + 1.6.0 ores.
 */
public final class ElectronicsRecipes {

    private ElectronicsRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Vanilla / shared ingredients.
        RecipeIngredient iron = RecipeIngredient.of(Material.IRON_INGOT);
        RecipeIngredient gold = RecipeIngredient.of(Material.GOLD_INGOT);
        RecipeIngredient redstone = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient diamond = RecipeIngredient.of(Material.DIAMOND);
        RecipeIngredient quartz = RecipeIngredient.of(Material.QUARTZ);
        RecipeIngredient amethyst = RecipeIngredient.of(Material.AMETHYST_SHARD);
        RecipeIngredient netheriteScrap = RecipeIngredient.of(Material.NETHERITE_SCRAP);
        RecipeIngredient blaze = RecipeIngredient.of(Material.BLAZE_POWDER);
        RecipeIngredient ironBlock = new RecipeIngredient.Vanilla(Material.IRON_BLOCK, 1);
        RecipeIngredient mvCasing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing_mv"));
        RecipeIngredient cableT2 = RecipeIngredient.of(new NamespacedKey(plugin, "cable_t2"));
        RecipeIngredient cableT3 = RecipeIngredient.of(new NamespacedKey(plugin, "cable_t3"));
        RecipeIngredient piston = new RecipeIngredient.Vanilla(Material.PISTON, 1);

        // Metallurgy plates / wires.
        RecipeIngredient siliconDust  = ingot(plugin, Metal.SILICON,  MetalForm.DUST);
        RecipeIngredient siliconPlate = ingot(plugin, Metal.SILICON,  MetalForm.PLATE);
        RecipeIngredient aluminumPlate = ingot(plugin, Metal.ALUMINUM, MetalForm.PLATE);
        RecipeIngredient aluminumWire  = ingot(plugin, Metal.ALUMINUM, MetalForm.WIRE);
        RecipeIngredient titaniumPlate = ingot(plugin, Metal.TITANIUM, MetalForm.PLATE);
        RecipeIngredient titaniumIngot = ingot(plugin, Metal.TITANIUM, MetalForm.INGOT);
        RecipeIngredient lithiumDust  = ingot(plugin, Metal.LITHIUM,  MetalForm.DUST);
        RecipeIngredient stainlessPlate = ingot(plugin, Metal.STAINLESS_STEEL, MetalForm.PLATE);
        RecipeIngredient nichromeWire  = ingot(plugin, Metal.NICHROME, MetalForm.WIRE);
        RecipeIngredient damascusPlate = ingot(plugin, Metal.DAMASCUS_STEEL, MetalForm.PLATE);
        RecipeIngredient copperWire   = ingot(plugin, Metal.COPPER,   MetalForm.WIRE);

        // ---------- Components (T-422) -------------------------------------------------------

        // silicon_wafer: 4× silicon dust + 1× quartz → 4 wafers
        register(api, key(plugin, "recipe_silicon_wafer"),
                List.of(siliconDust, siliconDust, siliconDust,
                        siliconDust, quartz,      siliconDust,
                        siliconDust, siliconDust, siliconDust),
                componentStack(api, plugin, Component.SILICON_WAFER, 4),
                GuideCategory.MATERIAL);

        // motor_t1: iron + copper wire + redstone
        register(api, key(plugin, "recipe_motor_t1"),
                List.of(copperWire, iron,      copperWire,
                        copperWire, redstone,  copperWire,
                        copperWire, iron,      copperWire),
                componentStack(api, plugin, Component.MOTOR_T1, 1),
                GuideCategory.MATERIAL);

        // motor_t2: aluminum plate + motor_t1 + redstone
        RecipeIngredient motorT1 = component(plugin, Component.MOTOR_T1);
        register(api, key(plugin, "recipe_motor_t2"),
                List.of(aluminumPlate, aluminumPlate, aluminumPlate,
                        aluminumWire,  motorT1,       aluminumWire,
                        aluminumPlate, aluminumPlate, aluminumPlate),
                componentStack(api, plugin, Component.MOTOR_T2, 1),
                GuideCategory.MATERIAL);

        // motor_t3: titanium plate + motor_t2 + nichrome wire
        RecipeIngredient motorT2 = component(plugin, Component.MOTOR_T2);
        register(api, key(plugin, "recipe_motor_t3"),
                List.of(titaniumPlate, titaniumPlate, titaniumPlate,
                        nichromeWire,  motorT2,       nichromeWire,
                        titaniumPlate, titaniumPlate, titaniumPlate),
                componentStack(api, plugin, Component.MOTOR_T3, 1),
                GuideCategory.MATERIAL);

        // circuit_t1: silicon wafer + redstone + copper wire
        RecipeIngredient wafer = component(plugin, Component.SILICON_WAFER);
        register(api, key(plugin, "recipe_circuit_t1"),
                List.of(copperWire, redstone,  copperWire,
                        redstone,   wafer,     redstone,
                        copperWire, redstone,  copperWire),
                componentStack(api, plugin, Component.CIRCUIT_T1, 1),
                GuideCategory.MATERIAL);

        // circuit_t2: silicon wafer + circuit_t1 + gold
        RecipeIngredient circuitT1 = component(plugin, Component.CIRCUIT_T1);
        register(api, key(plugin, "recipe_circuit_t2"),
                List.of(gold,        circuitT1, gold,
                        circuitT1,   wafer,     circuitT1,
                        gold,        circuitT1, gold),
                componentStack(api, plugin, Component.CIRCUIT_T2, 1),
                GuideCategory.MATERIAL);

        // circuit_t3: silicon wafer + circuit_t2 + diamond
        RecipeIngredient circuitT2 = component(plugin, Component.CIRCUIT_T2);
        register(api, key(plugin, "recipe_circuit_t3"),
                List.of(diamond,    circuitT2, diamond,
                        circuitT2,  wafer,     circuitT2,
                        diamond,    circuitT2, diamond),
                componentStack(api, plugin, Component.CIRCUIT_T3, 1),
                GuideCategory.MATERIAL);

        // processor_t1: 4× wafer + circuit_t1 + diamond
        register(api, key(plugin, "recipe_processor_t1"),
                List.of(wafer,     circuitT1, wafer,
                        circuitT1, diamond,   circuitT1,
                        wafer,     circuitT1, wafer),
                componentStack(api, plugin, Component.PROCESSOR_T1, 1),
                GuideCategory.MATERIAL);

        // processor_t2: processor_t1 + circuit_t2 + amethyst
        RecipeIngredient processorT1 = component(plugin, Component.PROCESSOR_T1);
        register(api, key(plugin, "recipe_processor_t2"),
                List.of(amethyst,   circuitT2, amethyst,
                        circuitT2,  processorT1, circuitT2,
                        amethyst,   circuitT2, amethyst),
                componentStack(api, plugin, Component.PROCESSOR_T2, 1),
                GuideCategory.MATERIAL);

        // processor_t3: processor_t2 + circuit_t3 + netherite scrap
        RecipeIngredient processorT2 = component(plugin, Component.PROCESSOR_T2);
        register(api, key(plugin, "recipe_processor_t3"),
                List.of(netheriteScrap, circuitT3(plugin), netheriteScrap,
                        circuitT3(plugin), processorT2,    circuitT3(plugin),
                        netheriteScrap, circuitT3(plugin), netheriteScrap),
                componentStack(api, plugin, Component.PROCESSOR_T3, 1),
                GuideCategory.MATERIAL);

        // coil_t1..t3: progressively better wire wraps
        register(api, key(plugin, "recipe_coil_t1"),
                List.of(copperWire, copperWire, copperWire,
                        copperWire, iron,       copperWire,
                        copperWire, copperWire, copperWire),
                componentStack(api, plugin, Component.COIL_T1, 1),
                GuideCategory.MATERIAL);

        register(api, key(plugin, "recipe_coil_t2"),
                List.of(aluminumWire, aluminumWire, aluminumWire,
                        aluminumWire, component(plugin, Component.COIL_T1), aluminumWire,
                        aluminumWire, aluminumWire, aluminumWire),
                componentStack(api, plugin, Component.COIL_T2, 1),
                GuideCategory.MATERIAL);

        register(api, key(plugin, "recipe_coil_t3"),
                List.of(nichromeWire, nichromeWire, nichromeWire,
                        nichromeWire, component(plugin, Component.COIL_T2), nichromeWire,
                        nichromeWire, nichromeWire, nichromeWire),
                componentStack(api, plugin, Component.COIL_T3, 1),
                GuideCategory.MATERIAL);

        // ram_t2 / ram_t3: layered circuits
        register(api, key(plugin, "recipe_ram_t2"),
                List.of(circuitT2,  wafer,     circuitT2,
                        gold,       wafer,     gold,
                        circuitT2,  wafer,     circuitT2),
                componentStack(api, plugin, Component.RAM_T2, 1),
                GuideCategory.MATERIAL);

        register(api, key(plugin, "recipe_ram_t3"),
                List.of(circuitT3(plugin), wafer, circuitT3(plugin),
                        diamond,           wafer, diamond,
                        circuitT3(plugin), wafer, circuitT3(plugin)),
                componentStack(api, plugin, Component.RAM_T3, 1),
                GuideCategory.MATERIAL);

        // storage_hdd / storage_ssd
        register(api, key(plugin, "recipe_storage_hdd"),
                List.of(iron,       circuitT1, iron,
                        circuitT1,  iron,      circuitT1,
                        iron,       circuitT1, iron),
                componentStack(api, plugin, Component.STORAGE_HDD, 1),
                GuideCategory.MATERIAL);

        register(api, key(plugin, "recipe_storage_ssd"),
                List.of(component(plugin, Component.RAM_T2), circuitT2, component(plugin, Component.RAM_T2),
                        circuitT2, component(plugin, Component.STORAGE_HDD), circuitT2,
                        component(plugin, Component.RAM_T2), circuitT2, component(plugin, Component.RAM_T2)),
                componentStack(api, plugin, Component.STORAGE_SSD, 1),
                GuideCategory.MATERIAL);

        // ---------- HV energy (T-425) --------------------------------------------------------

        // cable_t3: titanium wires around aluminum core (4× output)
        register(api, key(plugin, "recipe_cable_t3"),
                List.of(nichromeWire, nichromeWire, nichromeWire,
                        nichromeWire, aluminumWire, nichromeWire,
                        nichromeWire, nichromeWire, nichromeWire),
                sapientiaStack(api, key(plugin, "cable_t3"), 4),
                GuideCategory.ENERGY);

        // capacitor_t3: HV-tier bulk buffer
        register(api, key(plugin, "recipe_capacitor_t3"),
                List.of(stainlessPlate, component(plugin, Component.RAM_T3), stainlessPlate,
                        component(plugin, Component.COIL_T3), diamond,       component(plugin, Component.COIL_T3),
                        stainlessPlate, component(plugin, Component.RAM_T3), stainlessPlate),
                sapientiaStack(api, key(plugin, "capacitor_t3"), 1),
                GuideCategory.ENERGY);

        // transformer_mv_hv
        register(api, key(plugin, "recipe_transformer_mv_hv"),
                List.of(stainlessPlate, component(plugin, Component.COIL_T2), stainlessPlate,
                        cableT2,        mvCasing,    cableT3,
                        stainlessPlate, component(plugin, Component.COIL_T3), stainlessPlate),
                sapientiaStack(api, key(plugin, "transformer_mv_hv"), 1),
                GuideCategory.ENERGY);

        // geothermal_gen
        register(api, key(plugin, "recipe_geothermal_gen"),
                List.of(stainlessPlate, blaze,           stainlessPlate,
                        component(plugin, Component.COIL_T3), mvCasing, component(plugin, Component.COIL_T3),
                        stainlessPlate, blaze,           stainlessPlate),
                sapientiaStack(api, key(plugin, "geothermal_gen"), 1),
                GuideCategory.ENERGY);

        // gas_turbine
        register(api, key(plugin, "recipe_gas_turbine"),
                List.of(damascusPlate, motorT2,                   damascusPlate,
                        component(plugin, Component.COIL_T3), mvCasing, component(plugin, Component.COIL_T3),
                        damascusPlate, motorT2,                   damascusPlate),
                sapientiaStack(api, key(plugin, "gas_turbine"), 1),
                GuideCategory.ENERGY);

        // rtg
        register(api, key(plugin, "recipe_rtg"),
                List.of(stainlessPlate, lithiumDust, stainlessPlate,
                        lithiumDust,    mvCasing,    lithiumDust,
                        stainlessPlate, lithiumDust, stainlessPlate),
                sapientiaStack(api, key(plugin, "rtg"), 1),
                GuideCategory.ENERGY);

        // ---------- HV machines (T-423) ------------------------------------------------------

        register(api, key(plugin, "recipe_electrolyzer"),
                List.of(titaniumPlate, ironBlock,                 titaniumPlate,
                        cableT3,       mvCasing,                  cableT3,
                        titaniumPlate, component(plugin, Component.RAM_T2), titaniumPlate),
                sapientiaStack(api, key(plugin, "electrolyzer"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_rolling_mill"),
                List.of(stainlessPlate, motorT2,                   stainlessPlate,
                        piston,         mvCasing,                  piston,
                        stainlessPlate, component(plugin, Component.COIL_T2), stainlessPlate),
                sapientiaStack(api, key(plugin, "rolling_mill"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_laser_cutter"),
                List.of(damascusPlate, amethyst,                  damascusPlate,
                        diamond,       mvCasing,                  diamond,
                        damascusPlate, component(plugin, Component.RAM_T3), damascusPlate),
                sapientiaStack(api, key(plugin, "laser_cutter"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_chemical_reactor"),
                List.of(titaniumPlate, blaze,                     titaniumPlate,
                        siliconPlate,  mvCasing,                  siliconPlate,
                        titaniumPlate, component(plugin, Component.COIL_T3), titaniumPlate),
                sapientiaStack(api, key(plugin, "chemical_reactor"), 1),
                GuideCategory.MACHINE);

        // ---------- Gas blocks (T-426) -------------------------------------------------------

        register(api, key(plugin, "recipe_pressurized_pipe"),
                List.of(stainlessPlate, stainlessPlate, stainlessPlate,
                        nichromeWire,   piston,         nichromeWire,
                        stainlessPlate, stainlessPlate, stainlessPlate),
                sapientiaStack(api, key(plugin, "pressurized_pipe"), 8),
                GuideCategory.LOGISTICS);

        register(api, key(plugin, "recipe_gas_compressor"),
                List.of(stainlessPlate, motorT2,        stainlessPlate,
                        piston,         mvCasing,       piston,
                        stainlessPlate, motorT2,        stainlessPlate),
                sapientiaStack(api, key(plugin, "gas_compressor"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_boiler"),
                List.of(titaniumPlate, blaze,        titaniumPlate,
                        cableT2,       mvCasing,     cableT2,
                        titaniumPlate, blaze,        titaniumPlate),
                sapientiaStack(api, key(plugin, "boiler"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_condenser"),
                List.of(stainlessPlate, ingotIce(),                  stainlessPlate,
                        cableT2,        mvCasing,                    cableT2,
                        stainlessPlate, ingotIce(),                  stainlessPlate),
                sapientiaStack(api, key(plugin, "condenser"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_liquefier"),
                List.of(damascusPlate, ingotPackedIce(),             damascusPlate,
                        cableT3,       mvCasing,                     cableT3,
                        damascusPlate, ingotPackedIce(),             damascusPlate),
                sapientiaStack(api, key(plugin, "liquefier"), 1),
                GuideCategory.MACHINE);

        register(api, key(plugin, "recipe_phase_separator"),
                List.of(damascusPlate, motorT2,                       damascusPlate,
                        cableT3,       mvCasing,                      cableT3,
                        damascusPlate, component(plugin, Component.COIL_T3), damascusPlate),
                sapientiaStack(api, key(plugin, "phase_separator"), 1),
                GuideCategory.MACHINE);
    }

    // ---------- helpers ---------------------------------------------------------------------

    private static RecipeIngredient component(Plugin plugin, Component c) {
        return RecipeIngredient.of(new NamespacedKey(plugin, c.idBase()));
    }

    private static RecipeIngredient circuitT3(Plugin plugin) {
        return component(plugin, Component.CIRCUIT_T3);
    }

    private static RecipeIngredient ingot(Plugin plugin, Metal metal, MetalForm form) {
        return RecipeIngredient.of(MetalCatalog.idOf(plugin, metal, form));
    }

    private static RecipeIngredient ingotIce() {
        return new RecipeIngredient.Vanilla(Material.ICE, 1);
    }

    private static RecipeIngredient ingotPackedIce() {
        return new RecipeIngredient.Vanilla(Material.PACKED_ICE, 1);
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    private static ItemStack componentStack(SapientiaAPI api, Plugin plugin, Component c, int amount) {
        return api.createStack(new NamespacedKey(plugin, c.idBase()), amount).orElseThrow(() ->
                new IllegalStateException("Sapientia component not registered: " + c.idBase()));
    }

    private static ItemStack sapientiaStack(SapientiaAPI api, NamespacedKey itemId, int amount) {
        return api.createStack(itemId, amount).orElseThrow(() ->
                new IllegalStateException("Sapientia item not registered: " + itemId));
    }

    private static void register(SapientiaAPI api, NamespacedKey id, List<RecipeIngredient> pattern,
                                 ItemStack result, GuideCategory category) {
        api.recipes().register(new BundledRecipe(id, pattern, result, category));
    }
}
