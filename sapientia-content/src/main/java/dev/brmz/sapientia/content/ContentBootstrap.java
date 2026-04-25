package dev.brmz.sapientia.content;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.content.blocks.SapientiaConsole;
import dev.brmz.sapientia.content.blocks.SapientiaPedestal;
import dev.brmz.sapientia.content.crafting.BundledRecipes;
import dev.brmz.sapientia.content.crafting.SapientiaWorkbench;
import dev.brmz.sapientia.content.energy.EnergyInspector;
import dev.brmz.sapientia.content.energy.SapientiaCable;
import dev.brmz.sapientia.content.energy.SapientiaCableT2;
import dev.brmz.sapientia.content.energy.SapientiaCapacitor;
import dev.brmz.sapientia.content.energy.SapientiaCapacitorT2;
import dev.brmz.sapientia.content.energy.SapientiaConsumer;
import dev.brmz.sapientia.content.energy.SapientiaGenerator;
import dev.brmz.sapientia.content.energy.SapientiaTransformerLvMv;
import dev.brmz.sapientia.content.fluids.SapientiaFluidDrain;
import dev.brmz.sapientia.content.fluids.SapientiaFluidPipe;
import dev.brmz.sapientia.content.fluids.SapientiaFluidPump;
import dev.brmz.sapientia.content.fluids.SapientiaFluidTank;
import dev.brmz.sapientia.content.items.SapientiaGuide;
import dev.brmz.sapientia.content.items.SapientiaWrench;
import dev.brmz.sapientia.content.logistics.SapientiaItemCable;
import dev.brmz.sapientia.content.logistics.SapientiaItemConsumer;
import dev.brmz.sapientia.content.logistics.SapientiaItemFilter;
import dev.brmz.sapientia.content.logistics.SapientiaItemProducer;
import dev.brmz.sapientia.content.machines.SapientiaBenchSaw;
import dev.brmz.sapientia.content.machines.SapientiaCompressor;
import dev.brmz.sapientia.content.machines.SapientiaElectricFurnace;
import dev.brmz.sapientia.content.machines.SapientiaExtractor;
import dev.brmz.sapientia.content.machines.SapientiaMacerator;
import dev.brmz.sapientia.content.machines.SapientiaMixer;
import dev.brmz.sapientia.content.machines.SapientiaOreWasher;
import dev.brmz.sapientia.content.machines.SapientiaPlatePress;
import dev.brmz.sapientia.content.metallurgy.MetalCatalog;
import dev.brmz.sapientia.content.multiblock.SapientiaInductionFurnaceController;
import dev.brmz.sapientia.content.multiblock.SapientiaMachineCasing;
import dev.brmz.sapientia.content.multiblock.SapientiaMachineCasingMv;
import dev.brmz.sapientia.content.chemistry.SapientiaBioreactor;
import dev.brmz.sapientia.content.chemistry.SapientiaCracker;
import dev.brmz.sapientia.content.chemistry.SapientiaFermenter;
import dev.brmz.sapientia.content.chemistry.SapientiaStill;
import dev.brmz.sapientia.content.petroleum.SapientiaBiogasGen;
import dev.brmz.sapientia.content.petroleum.SapientiaCombustionGen;
import dev.brmz.sapientia.content.petroleum.SapientiaOilRefineryController;
import dev.brmz.sapientia.content.petroleum.SapientiaPumpjack;
import dev.brmz.sapientia.content.petroleum.SapientiaStainlessSteelCasing;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the bundled Sapientia catalog (items, blocks, recipes) against the
 * public {@link SapientiaAPI}. Called from {@code SapientiaPlugin#onEnable}
 * after the registries are ready. See ADR-012 and ROADMAP T-180 / T-143 / T-131.
 */
public final class ContentBootstrap {

    private ContentBootstrap() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Items
        api.registerItem(new SapientiaWrench(plugin));
        api.registerItem(new SapientiaGuide(plugin));

        // Blocks (decorative + demo)
        api.registerBlock(new SapientiaPedestal(plugin));
        api.registerBlock(new SapientiaConsole(plugin));

        // Energy demo (T-143 / 0.3.0)
        api.registerBlock(new SapientiaGenerator(plugin));
        api.registerBlock(new SapientiaCable(plugin));
        api.registerBlock(new SapientiaCapacitor(plugin));
        api.registerBlock(new SapientiaConsumer(plugin));

        // Item logistics demo (T-300 / 1.1.0)
        api.registerBlock(new SapientiaItemCable(plugin));
        api.registerBlock(new SapientiaItemProducer(plugin));
        api.registerBlock(new SapientiaItemConsumer(plugin));
        api.registerBlock(new SapientiaItemFilter(plugin));

        // Fluid logistics demo (T-301 / 1.2.0)
        api.registerBlock(new SapientiaFluidPipe(plugin));
        api.registerBlock(new SapientiaFluidPump(plugin));
        api.registerBlock(new SapientiaFluidTank(plugin));
        api.registerBlock(new SapientiaFluidDrain(plugin));

        // Crafting (T-130 / 0.4.0)
        api.registerBlock(new SapientiaWorkbench(plugin));
        BundledRecipes.registerAll(plugin, api);

        // Metallurgy items (T-402 / T-403 / 1.4.0) — 78 metal items.
        MetalCatalog.registerAll(plugin, api);

        // Shared casings (T-400 / T-405 / 1.4.0).
        api.registerBlock(new SapientiaMachineCasing(plugin));
        api.registerBlock(new SapientiaMachineCasingMv(plugin));

        // MV energy expansion (T-406 / 1.4.0).
        api.registerBlock(new SapientiaCableT2(plugin));
        api.registerBlock(new SapientiaCapacitorT2(plugin));
        api.registerBlock(new SapientiaTransformerLvMv(plugin));

        // LV / MV machines (T-404 / 1.4.0).
        api.registerBlock(new SapientiaMacerator(plugin));
        api.registerBlock(new SapientiaOreWasher(plugin));
        api.registerBlock(new SapientiaElectricFurnace(plugin));
        api.registerBlock(new SapientiaMixer(plugin));
        api.registerBlock(new SapientiaCompressor(plugin));
        api.registerBlock(new SapientiaBenchSaw(plugin));
        api.registerBlock(new SapientiaPlatePress(plugin));
        api.registerBlock(new SapientiaExtractor(plugin));

        // Multiblock controller (T-405 / 1.4.0).
        api.registerBlock(new SapientiaInductionFurnaceController(plugin));

        // 1.4.0 recipes (must come after items + blocks are registered).
        dev.brmz.sapientia.content.crafting.MetallurgyRecipes.registerAll(plugin, api);

        // Petroleum & basic chemistry (T-411..T-415 / 1.5.0).
        api.registerBlock(new SapientiaStainlessSteelCasing(plugin));
        api.registerBlock(new SapientiaPumpjack(plugin));
        api.registerBlock(new SapientiaCombustionGen(plugin));
        api.registerBlock(new SapientiaBiogasGen(plugin));
        api.registerBlock(new SapientiaCracker(plugin));
        api.registerBlock(new SapientiaFermenter(plugin));
        api.registerBlock(new SapientiaStill(plugin));
        api.registerBlock(new SapientiaBioreactor(plugin));
        api.registerBlock(new SapientiaOilRefineryController(plugin));

        // 1.5.0 recipes.
        dev.brmz.sapientia.content.crafting.PetrochemRecipes.registerAll(plugin, api);

        // Look-to-inspect loop for the wrench (action bar / boss bar)
        new EnergyInspector(plugin).start();
    }
}
