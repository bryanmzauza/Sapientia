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
import dev.brmz.sapientia.content.items.SapientiaGuide;
import dev.brmz.sapientia.content.items.SapientiaWrench;
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

        // Crafting (T-130 / 0.4.0)
        api.registerBlock(new SapientiaWorkbench(plugin));
        BundledRecipes.registerAll(plugin, api);

        // Look-to-inspect loop for the wrench (action bar / boss bar)
        new EnergyInspector(plugin).start();
    }
}
