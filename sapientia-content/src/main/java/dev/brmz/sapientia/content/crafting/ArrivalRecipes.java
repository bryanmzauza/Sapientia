package dev.brmz.sapientia.content.crafting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.crafting.VanillaRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Era 0 (Arrival): the items made on the vanilla crafting table, before the
 * Sapientia Workbench exists. See {@code docs/eras/era-00-chegada.md}.
 */
public final class ArrivalRecipes {

    private ArrivalRecipes() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        // Guide: 1 book + 1 flint, shapeless.
        api.recipes().registerVanilla(VanillaRecipe.shapeless(
                new NamespacedKey(plugin, "vanilla_guide"), new NamespacedKey(plugin, "guide"), 1,
                Set.of(Material.BOOK), Set.of(Material.FLINT)));

        // Era almanac: 1 book + 1 flint + 1 paper, shapeless.
        api.recipes().registerVanilla(VanillaRecipe.shapeless(
                new NamespacedKey(plugin, "vanilla_era_almanac"), new NamespacedKey(plugin, "era_almanac"), 1,
                Set.of(Material.BOOK), Set.of(Material.FLINT), Set.of(Material.PAPER)));

        // Workbench: crafting table in the centre, cobblestone in the corners,
        // planks above and below, flint on the sides.
        api.recipes().registerVanilla(VanillaRecipe.shaped(
                new NamespacedKey(plugin, "vanilla_workbench"), new NamespacedKey(plugin, "workbench"), 1,
                List.of("CPC", "FTF", "CPC"),
                Map.of('C', Tag.ITEMS_STONE_CRAFTING_MATERIALS.getValues(),
                        'P', Tag.ITEMS_PLANKS.getValues(),
                        'F', Set.of(Material.FLINT),
                        'T', Set.of(Material.CRAFTING_TABLE))));
    }
}
