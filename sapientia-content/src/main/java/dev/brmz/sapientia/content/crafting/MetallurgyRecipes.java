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
 * 1.4.0 metallurgy + MV-tier recipes (T-402 / T-403 / T-404 / T-406).
 *
 * <p>Covers the exit-gate vertical slice:
 * <pre>
 *   raw → dust (workbench: 1× raw → 1× dust)        — placeholder until macerator processes
 *   dust → ingot (workbench: 1× dust → 1× ingot)    — placeholder until electric furnace processes
 *   9× ingot → block (workbench)                    — every metal
 *   block → 9× ingot (workbench, stub via 1× block) — skipped (use unboxing later)
 *   alloys: copper+tin → bronze, copper+zinc → brass, gold+silver → electrum
 *   ingot → plate (workbench, hammer-style)         — every metal
 *   ingot → wire (workbench: ingot+ingot top → 4× wire)
 *   2× wire + 1× rubber? → cable_t2 (skipped, use vanilla quartz proxy)
 *   machine_casing: 8× iron + 1× redstone block
 *   machine_casing_mv: 8× plate (any) + 1× iron block
 *   transformer_lv_mv: machine_casing_mv core + 4× copper wire + 4× iron ingot
 *   cable_t2: copper plate top row → 8× cable_t2 (analogous to LV cable recipe)
 * </pre>
 */
public final class MetallurgyRecipes {

    private MetallurgyRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        registerOreToDustAndIngot(plugin, api);
        registerStorageBlocks(plugin, api);
        registerAlloys(plugin, api);
        registerPlatesAndWires(plugin, api);
        registerCasings(plugin, api);
        registerMvEnergy(plugin, api);
        registerMachineRecipes(plugin, api);
    }

    // --- raw → dust → ingot (placeholder workbench recipes) -------------------------------------

    private static void registerOreToDustAndIngot(Plugin plugin, SapientiaAPI api) {
        for (Metal metal : Metal.values()) {
            if (metal.isAlloy()) continue;
            NamespacedKey raw   = idOf(plugin, metal, MetalForm.RAW);
            NamespacedKey dust  = idOf(plugin, metal, MetalForm.DUST);
            NamespacedKey ingot = idOf(plugin, metal, MetalForm.INGOT);

            register(api, new NamespacedKey(plugin, "recipe_" + metal.idBase() + "_raw_to_dust"),
                    List.of(
                            empty(), RecipeIngredient.of(raw), empty(),
                            empty(), empty(),                  empty(),
                            empty(), empty(),                  empty()),
                    sapientiaStack(api, dust, 1),
                    GuideCategory.MATERIAL);

            register(api, new NamespacedKey(plugin, "recipe_" + metal.idBase() + "_dust_to_ingot"),
                    List.of(
                            empty(), RecipeIngredient.of(dust), empty(),
                            empty(), empty(),                   empty(),
                            empty(), empty(),                   empty()),
                    sapientiaStack(api, ingot, 1),
                    GuideCategory.MATERIAL);
        }
    }

    // --- 9-ingot storage blocks ------------------------------------------------------------------

    private static void registerStorageBlocks(Plugin plugin, SapientiaAPI api) {
        for (Metal metal : Metal.values()) {
            NamespacedKey ingot = idOf(plugin, metal, MetalForm.INGOT);
            NamespacedKey block = idOf(plugin, metal, MetalForm.BLOCK);
            RecipeIngredient i = RecipeIngredient.of(ingot);

            register(api, new NamespacedKey(plugin, "recipe_" + metal.idBase() + "_block"),
                    List.of(
                            i, i, i,
                            i, i, i,
                            i, i, i),
                    sapientiaStack(api, block, 1),
                    GuideCategory.MATERIAL);
        }
    }

    // --- alloys (LV mixer placeholder via workbench) --------------------------------------------

    private static void registerAlloys(Plugin plugin, SapientiaAPI api) {
        // bronze: 3× copper dust + 1× tin dust → 4× bronze dust
        registerAlloy(plugin, api, "bronze", Metal.COPPER, 3, Metal.TIN, 1, Metal.BRONZE, 4);
        // brass:  3× copper dust + 1× zinc dust → 4× brass dust
        registerAlloy(plugin, api, "brass",  Metal.COPPER, 3, Metal.ZINC, 1, Metal.BRASS, 4);
        // electrum: 1× silver dust + 1× gold (proxy: nickel for now) — use 2× silver + 2× nickel → 4× electrum dust
        registerAlloy(plugin, api, "electrum", Metal.SILVER, 2, Metal.NICKEL, 2, Metal.ELECTRUM, 4);
    }

    private static void registerAlloy(
            Plugin plugin, SapientiaAPI api, String alloyId,
            Metal a, int aCount, Metal b, int bCount, Metal product, int productCount) {
        NamespacedKey aDust = idOf(plugin, a, MetalForm.DUST);
        NamespacedKey bDust = idOf(plugin, b, MetalForm.DUST);
        NamespacedKey out   = idOf(plugin, product, MetalForm.DUST);

        // Layout: aCount cells of metal A, then bCount cells of metal B, fill rest with empty.
        RecipeIngredient[] grid = new RecipeIngredient[9];
        int idx = 0;
        for (int n = 0; n < aCount && idx < 9; n++, idx++) grid[idx] = RecipeIngredient.of(aDust);
        for (int n = 0; n < bCount && idx < 9; n++, idx++) grid[idx] = RecipeIngredient.of(bDust);
        for (; idx < 9; idx++) grid[idx] = empty();

        register(api, new NamespacedKey(plugin, "recipe_alloy_" + alloyId),
                List.of(grid),
                sapientiaStack(api, out, productCount),
                GuideCategory.MATERIAL);
    }

    // --- plates + wires (workbench placeholders) -------------------------------------------------

    private static void registerPlatesAndWires(Plugin plugin, SapientiaAPI api) {
        for (Metal metal : Metal.values()) {
            NamespacedKey ingot = idOf(plugin, metal, MetalForm.INGOT);
            NamespacedKey plate = idOf(plugin, metal, MetalForm.PLATE);
            NamespacedKey wire  = idOf(plugin, metal, MetalForm.WIRE);
            RecipeIngredient i = RecipeIngredient.of(ingot);

            // Plate: vertical stack of 3 ingots → 1 plate
            register(api, new NamespacedKey(plugin, "recipe_" + metal.idBase() + "_plate"),
                    List.of(
                            empty(), i, empty(),
                            empty(), i, empty(),
                            empty(), i, empty()),
                    sapientiaStack(api, plate, 1),
                    GuideCategory.MATERIAL);

            // Wire: top row of 3 ingots → 4 wires
            register(api, new NamespacedKey(plugin, "recipe_" + metal.idBase() + "_wire"),
                    List.of(
                            i, i, i,
                            empty(), empty(), empty(),
                            empty(), empty(), empty()),
                    sapientiaStack(api, wire, 4),
                    GuideCategory.MATERIAL);
        }
    }

    // --- shared machine casings ------------------------------------------------------------------

    private static void registerCasings(Plugin plugin, SapientiaAPI api) {
        RecipeIngredient iron = RecipeIngredient.of(Material.IRON_INGOT);
        RecipeIngredient redstoneBlock = new RecipeIngredient.Vanilla(Material.REDSTONE_BLOCK, 1);

        register(api, new NamespacedKey(plugin, "recipe_machine_casing"),
                List.of(
                        iron, iron,          iron,
                        iron, redstoneBlock, iron,
                        iron, iron,          iron),
                sapientiaStack(api, new NamespacedKey(plugin, "machine_casing"), 1),
                GuideCategory.MATERIAL);

        // MV casing: 4× copper plate + 4× iron ingot + 1× iron block
        RecipeIngredient copperPlate = RecipeIngredient.of(idOf(plugin, Metal.COPPER, MetalForm.PLATE));
        RecipeIngredient ironBlock = new RecipeIngredient.Vanilla(Material.IRON_BLOCK, 1);
        register(api, new NamespacedKey(plugin, "recipe_machine_casing_mv"),
                List.of(
                        copperPlate, iron,      copperPlate,
                        iron,        ironBlock, iron,
                        copperPlate, iron,      copperPlate),
                sapientiaStack(api, new NamespacedKey(plugin, "machine_casing_mv"), 1),
                GuideCategory.MATERIAL);
    }

    // --- MV energy expansion: cable_t2, capacitor_t2, transformer ------------------------------

    private static void registerMvEnergy(Plugin plugin, SapientiaAPI api) {
        RecipeIngredient copperWire = RecipeIngredient.of(idOf(plugin, Metal.COPPER, MetalForm.WIRE));
        RecipeIngredient bronzePlate = RecipeIngredient.of(idOf(plugin, Metal.BRONZE, MetalForm.PLATE));
        RecipeIngredient iron = RecipeIngredient.of(Material.IRON_INGOT);
        RecipeIngredient redstone = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient copperBlock = RecipeIngredient.of(idOf(plugin, Metal.COPPER, MetalForm.BLOCK));
        RecipeIngredient mvCasing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing_mv"));

        // cable_t2: 3× copper wire top row → 8× cable_t2
        register(api, new NamespacedKey(plugin, "recipe_cable_t2"),
                List.of(
                        copperWire, redstone,  copperWire,
                        empty(),    empty(),   empty(),
                        empty(),    empty(),   empty()),
                sapientiaStack(api, new NamespacedKey(plugin, "cable_t2"), 8),
                GuideCategory.ENERGY);

        // capacitor_t2: bronze plates around a copper block
        register(api, new NamespacedKey(plugin, "recipe_capacitor_t2"),
                List.of(
                        bronzePlate, bronzePlate, bronzePlate,
                        bronzePlate, copperBlock, bronzePlate,
                        bronzePlate, bronzePlate, bronzePlate),
                sapientiaStack(api, new NamespacedKey(plugin, "capacitor_t2"), 1),
                GuideCategory.ENERGY);

        // transformer_lv_mv: copper wire on sides of MV casing
        register(api, new NamespacedKey(plugin, "recipe_transformer_lv_mv"),
                List.of(
                        copperWire, iron,     copperWire,
                        copperWire, mvCasing, copperWire,
                        copperWire, iron,     copperWire),
                sapientiaStack(api, new NamespacedKey(plugin, "transformer_lv_mv"), 1),
                GuideCategory.ENERGY);
    }

    // --- machine recipes -------------------------------------------------------------------------

    private static void registerMachineRecipes(Plugin plugin, SapientiaAPI api) {
        RecipeIngredient casing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing"));
        RecipeIngredient mvCasing = RecipeIngredient.of(new NamespacedKey(plugin, "machine_casing_mv"));
        RecipeIngredient iron = RecipeIngredient.of(Material.IRON_INGOT);
        RecipeIngredient redstone = RecipeIngredient.of(Material.REDSTONE);
        RecipeIngredient piston = new RecipeIngredient.Vanilla(Material.PISTON, 1);
        RecipeIngredient furnace = new RecipeIngredient.Vanilla(Material.FURNACE, 1);
        RecipeIngredient blastFurnace = new RecipeIngredient.Vanilla(Material.BLAST_FURNACE, 1);
        RecipeIngredient cauldron = new RecipeIngredient.Vanilla(Material.CAULDRON, 1);
        RecipeIngredient anvil = new RecipeIngredient.Vanilla(Material.ANVIL, 1);
        RecipeIngredient stonecutter = new RecipeIngredient.Vanilla(Material.STONECUTTER, 1);
        RecipeIngredient observer = new RecipeIngredient.Vanilla(Material.OBSERVER, 1);
        RecipeIngredient pot = new RecipeIngredient.Vanilla(Material.DECORATED_POT, 1);

        // macerator (LV): casing + piston core
        register(api, new NamespacedKey(plugin, "recipe_macerator"),
                List.of(
                        iron,     piston,   iron,
                        redstone, casing,   redstone,
                        iron,     piston,   iron),
                sapientiaStack(api, new NamespacedKey(plugin, "macerator"), 1),
                GuideCategory.MACHINE);

        // ore_washer (LV): casing + cauldron core
        register(api, new NamespacedKey(plugin, "recipe_ore_washer"),
                List.of(
                        iron,     cauldron, iron,
                        redstone, casing,   redstone,
                        iron,     iron,     iron),
                sapientiaStack(api, new NamespacedKey(plugin, "ore_washer"), 1),
                GuideCategory.MACHINE);

        // electric_furnace (LV): casing + blast furnace core
        register(api, new NamespacedKey(plugin, "recipe_electric_furnace"),
                List.of(
                        iron,     blastFurnace, iron,
                        redstone, casing,       redstone,
                        iron,     furnace,      iron),
                sapientiaStack(api, new NamespacedKey(plugin, "electric_furnace"), 1),
                GuideCategory.MACHINE);

        // bench_saw (LV)
        register(api, new NamespacedKey(plugin, "recipe_bench_saw"),
                List.of(
                        iron,     stonecutter, iron,
                        redstone, casing,      redstone,
                        iron,     iron,        iron),
                sapientiaStack(api, new NamespacedKey(plugin, "bench_saw"), 1),
                GuideCategory.MACHINE);

        // mixer (MV)
        register(api, new NamespacedKey(plugin, "recipe_mixer"),
                List.of(
                        iron,     pot,      iron,
                        redstone, mvCasing, redstone,
                        iron,     iron,     iron),
                sapientiaStack(api, new NamespacedKey(plugin, "mixer"), 1),
                GuideCategory.MACHINE);

        // compressor (MV)
        register(api, new NamespacedKey(plugin, "recipe_compressor"),
                List.of(
                        iron,     piston,   iron,
                        redstone, mvCasing, redstone,
                        iron,     piston,   iron),
                sapientiaStack(api, new NamespacedKey(plugin, "compressor"), 1),
                GuideCategory.MACHINE);

        // plate_press (MV)
        register(api, new NamespacedKey(plugin, "recipe_plate_press"),
                List.of(
                        iron,     anvil,    iron,
                        redstone, mvCasing, redstone,
                        iron,     iron,     iron),
                sapientiaStack(api, new NamespacedKey(plugin, "plate_press"), 1),
                GuideCategory.MACHINE);

        // extractor (MV)
        register(api, new NamespacedKey(plugin, "recipe_extractor"),
                List.of(
                        iron,     observer, iron,
                        redstone, mvCasing, redstone,
                        iron,     iron,     iron),
                sapientiaStack(api, new NamespacedKey(plugin, "extractor"), 1),
                GuideCategory.MACHINE);

        // induction_furnace_controller (MV multiblock head)
        register(api, new NamespacedKey(plugin, "recipe_induction_furnace_controller"),
                List.of(
                        mvCasing, blastFurnace, mvCasing,
                        redstone, mvCasing,     redstone,
                        mvCasing, mvCasing,     mvCasing),
                sapientiaStack(api, new NamespacedKey(plugin, "induction_furnace_controller"), 1),
                GuideCategory.MACHINE);
    }

    // --- helpers ---------------------------------------------------------------------------------

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

    private static RecipeIngredient empty() { return RecipeIngredient.empty(); }
}
