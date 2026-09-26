package dev.brmz.sapientia.core.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.crafting.SmeltingRecipe;
import dev.brmz.sapientia.api.progression.ProgressionService;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Furnace rules for Sapientia items, all driven by vanilla furnace events (no
 * per-tick work):
 * <ul>
 *   <li>a Sapientia furnace (for example the clay furnace) only burns the fuels
 *       its block accepts;</li>
 *   <li>a recipe of a Sapientia furnace only runs in that block: other
 *       furnaces of the same kind neither burn fuel for it nor finish it;</li>
 *   <li>recipes whose result belongs to a locked era do not run;</li>
 *   <li>an input in a bucket (lye) leaves the empty bucket in the furnace.</li>
 * </ul>
 */
public final class FurnaceListener implements Listener {

    private final Plugin plugin;
    private final ChunkBlockIndex index;
    private final SapientiaRecipeRegistry recipes;
    private final ItemRegistry items;
    private final ProgressionService progression;
    private final Map<NamespacedKey, List<SmeltingRecipe>> byInput = new HashMap<>();

    public FurnaceListener(@NotNull Plugin plugin, @NotNull ChunkBlockIndex index,
                           @NotNull SapientiaRecipeRegistry recipes, @NotNull ItemRegistry items,
                           @NotNull ProgressionService progression) {
        this.plugin = plugin;
        this.index = index;
        this.recipes = recipes;
        this.items = items;
        this.progression = progression;
        for (SmeltingRecipe recipe : recipes.smeltingRecipes()) {
            byInput.computeIfAbsent(recipe.input(), k -> new ArrayList<>()).add(recipe);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(@NotNull FurnaceBurnEvent event) {
        Block block = event.getBlock();
        SapientiaBlock furnace = index.at(block);
        if (furnace != null && !furnace.furnaceFuels().isEmpty()
                && !furnace.furnaceFuels().contains(event.getFuel().getType())) {
            event.setCancelled(true);
            return;
        }
        if (block.getState(false) instanceof Furnace state && refuses(state.getInventory().getSmelting(), furnace)) {
            event.setCancelled(true); // do not waste fuel on something this furnace will not smelt
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStartSmelt(@NotNull FurnaceStartSmeltEvent event) {
        SmeltingRecipe recipe = recipes.smeltingRecipe(event.getRecipe().getKey());
        if (recipe != null && !allowed(recipe, index.at(event.getBlock()))) {
            event.setTotalCookTime(Integer.MAX_VALUE); // never finishes here
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmelt(@NotNull FurnaceSmeltEvent event) {
        if (event.getRecipe() == null) return;
        SmeltingRecipe recipe = recipes.smeltingRecipe(event.getRecipe().getKey());
        if (recipe == null) return;
        Block block = event.getBlock();
        if (!allowed(recipe, index.at(block))) {
            event.setCancelled(true);
            return;
        }
        Material container = event.getSource().getType().getCraftingRemainingItem();
        if (container != null && !container.isAir()) {
            // The input is used up after this event; hand the container back on the next tick.
            Bukkit.getScheduler().runTask(plugin, () -> returnContainer(block, container));
        }
    }

    private static void returnContainer(Block block, Material container) {
        if (!(block.getState(false) instanceof Furnace state)) return;
        FurnaceInventory inventory = state.getInventory();
        ItemStack empty = new ItemStack(container);
        if (inventory.getSmelting() == null || inventory.getSmelting().getType().isAir()) {
            inventory.setSmelting(empty);
        } else {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), empty);
        }
    }

    /** Whether {@code input} is a Sapientia item that this furnace has no usable recipe for. */
    private boolean refuses(@Nullable ItemStack input, @Nullable SapientiaBlock furnace) {
        String id = items.idOf(input);
        if (id == null) return false;
        NamespacedKey key = NamespacedKey.fromString(id);
        List<SmeltingRecipe> candidates = key == null ? null : byInput.get(key);
        if (candidates == null) return true; // Sapientia items never smelt as their vanilla base
        for (SmeltingRecipe recipe : candidates) {
            if (allowed(recipe, furnace)) return false;
        }
        return true;
    }

    private boolean allowed(@NotNull SmeltingRecipe recipe, @Nullable SapientiaBlock furnace) {
        if (!progression.isAvailable(recipe.result())) return false;
        if (recipe.furnace() == null) return true; // a furnace recipe: every furnace of that kind
        return furnace != null && furnace.id().equals(recipe.furnace());
    }
}
