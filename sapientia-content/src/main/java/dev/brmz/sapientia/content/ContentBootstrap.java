package dev.brmz.sapientia.content;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.content.blocks.SapientiaConsole;
import dev.brmz.sapientia.content.blocks.SapientiaPedestal;
import dev.brmz.sapientia.content.crafting.BundledRecipes;
import dev.brmz.sapientia.content.crafting.SapientiaWorkbench;
import dev.brmz.sapientia.content.energy.EnergyInspector;
import dev.brmz.sapientia.content.energy.SapientiaCable;
import dev.brmz.sapientia.content.energy.SapientiaCapacitor;
import dev.brmz.sapientia.content.energy.SapientiaConsumer;
import dev.brmz.sapientia.content.energy.SapientiaGenerator;
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

        // Look-to-inspect loop for the wrench (action bar / boss bar)
        new EnergyInspector(plugin).start();
    }
}
