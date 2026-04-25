package dev.brmz.sapientia.content.crafting;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.machine.MachineRecipe;
import dev.brmz.sapientia.api.machine.MachineRecipeRegistry;
import dev.brmz.sapientia.content.metallurgy.Metal;
import dev.brmz.sapientia.content.metallurgy.MetalCatalog;
import dev.brmz.sapientia.content.metallurgy.MetalForm;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Recipe catalogue consumed by {@code MachineProcessor}
 * (T-404 / T-405 / T-414 / 1.4.1 + 1.5.1).
 *
 * <p>Recipes are intentionally dense around the metallurgy & chemistry tiers
 * so the kinetic loop has something to chew on out of the box. Each metal
 * gets the canonical {@code raw → 2× dust} (macerator),
 * {@code dust → ingot} (electric furnace), {@code ingot → plate} (plate
 * press), {@code ingot → 4× wire} (extractor), and {@code ingot → rod}
 * (bench saw).
 */
public final class MachineRecipeData {

    private MachineRecipeData() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        MachineRecipeRegistry registry = api.machineRecipes();

        NamespacedKey macerator       = new NamespacedKey(plugin, "macerator");
        NamespacedKey oreWasher       = new NamespacedKey(plugin, "ore_washer");
        NamespacedKey electricFurnace = new NamespacedKey(plugin, "electric_furnace");
        NamespacedKey benchSaw        = new NamespacedKey(plugin, "bench_saw");
        NamespacedKey mixer           = new NamespacedKey(plugin, "mixer");
        NamespacedKey compressor      = new NamespacedKey(plugin, "compressor");
        NamespacedKey platePress      = new NamespacedKey(plugin, "plate_press");
        NamespacedKey extractor       = new NamespacedKey(plugin, "extractor");

        // Per-metal canonical recipes (raw metals only).
        for (Metal metal : Metal.values()) {
            if (metal.isAlloy()) continue;
            ItemStack raw   = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.RAW),   1);
            ItemStack dust1 = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.DUST),  1);
            ItemStack dust2 = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.DUST),  2);
            ItemStack ingot = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.INGOT), 1);
            ItemStack plate = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.PLATE), 1);
            ItemStack wire4 = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.WIRE),  4);
            ItemStack rod   = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.ROD),   1);

            // macerator: raw → 2× dust
            registry.register(new MachineRecipe(macerator, raw, dust2, 64L, 20));
            // ore_washer: dust → dust (placeholder cleansing; same item, faster)
            registry.register(new MachineRecipe(oreWasher, dust1.clone(), dust1.clone(), 32L, 10));
            // electric_furnace: dust → ingot
            registry.register(new MachineRecipe(electricFurnace, dust1.clone(), ingot, 64L, 20));
            // plate_press: ingot → plate
            registry.register(new MachineRecipe(platePress, ingot.clone(), plate, 96L, 30));
            // extractor: ingot → 4× wire
            registry.register(new MachineRecipe(extractor, ingot.clone(), wire4, 96L, 30));
            // bench_saw: ingot → rod
            registry.register(new MachineRecipe(benchSaw, ingot.clone(), rod, 80L, 25));
        }

        // Mixer: dust+dust alloy combinations (each recipe runs against a single input
        // slot — the simplified contract — so we register one per primary metal as a
        // placeholder. Once multi-input lands the entries can be replaced wholesale.)
        registerAlloy(registry, mixer, api, plugin, Metal.COPPER, Metal.BRONZE);
        registerAlloy(registry, mixer, api, plugin, Metal.COPPER, Metal.BRASS);
        registerAlloy(registry, mixer, api, plugin, Metal.SILVER, Metal.ELECTRUM);

        // Compressor: 9× ingot → block (kept symmetric with workbench shaped recipe).
        for (Metal metal : Metal.values()) {
            ItemStack ingot9 = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.INGOT), 9);
            ItemStack block  = stack(api, MetalCatalog.idOf(plugin, metal, MetalForm.BLOCK), 1);
            registry.register(new MachineRecipe(compressor, ingot9, block, 128L, 40));
        }

        registerChemistry(plugin, registry);
    }

    private static void registerAlloy(MachineRecipeRegistry registry, NamespacedKey machine,
                                      SapientiaAPI api, Plugin plugin,
                                      Metal primary, Metal alloy) {
        ItemStack input  = stack(api, MetalCatalog.idOf(plugin, primary, MetalForm.DUST), 2);
        ItemStack output = stack(api, MetalCatalog.idOf(plugin, alloy,   MetalForm.DUST), 1);
        registry.register(new MachineRecipe(machine, input, output, 96L, 30));
    }

    private static void registerChemistry(Plugin plugin, MachineRecipeRegistry registry) {
        NamespacedKey bioreactor = new NamespacedKey(plugin, "bioreactor");

        // Bioreactor: organic matter → bottle (placeholder for nutrient_broth handling
        // — fluid output goes to a dedicated tank node in 1.5.2; today we surface
        // a vanilla glass-bottle marker so the machine has a working item-tick).
        ItemStack organic = new ItemStack(Material.ROTTEN_FLESH, 1);
        ItemStack broth   = new ItemStack(Material.HONEY_BOTTLE, 1);
        registry.register(new MachineRecipe(bioreactor, organic, broth, 64L, 30));

        ItemStack kelpInput = new ItemStack(Material.DRIED_KELP, 4);
        ItemStack kelpOut   = new ItemStack(Material.HONEY_BOTTLE, 1);
        registry.register(new MachineRecipe(bioreactor, kelpInput, kelpOut, 64L, 25));

        // Fermenter / cracker / still: items only — fluid recipes for these machines
        // live in PetroleumTicker. Item recipes give the machines vanilla-side
        // throughput for cosmetic byproducts.
        NamespacedKey fermenter = new NamespacedKey(plugin, "fermenter");
        registry.register(new MachineRecipe(fermenter,
                new ItemStack(Material.WHEAT, 4), new ItemStack(Material.SUGAR, 1), 32L, 20));

        NamespacedKey still = new NamespacedKey(plugin, "still");
        registry.register(new MachineRecipe(still,
                new ItemStack(Material.SUGAR_CANE, 4), new ItemStack(Material.SUGAR, 2), 32L, 20));

        NamespacedKey cracker = new NamespacedKey(plugin, "cracker");
        registry.register(new MachineRecipe(cracker,
                new ItemStack(Material.COAL, 1), new ItemStack(Material.GUNPOWDER, 1), 96L, 25));
    }

    private static ItemStack stack(SapientiaAPI api, NamespacedKey itemId, int amount) {
        return api.createStack(itemId, amount).orElseThrow(() ->
                new IllegalStateException("Sapientia item not registered: " + itemId));
    }
}
