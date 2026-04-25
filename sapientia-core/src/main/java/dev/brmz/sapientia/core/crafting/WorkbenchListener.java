package dev.brmz.sapientia.core.crafting;

import java.util.Arrays;

import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.events.SapientiaRecipeCompleteEvent;
import dev.brmz.sapientia.api.guide.UnlockService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatches {@link InventoryClickEvent} / {@link InventoryDragEvent} /
 * {@link InventoryCloseEvent} for {@link WorkbenchHolder} inventories (T-130 / 0.4.0).
 *
 * <p>Grid slots accept and release items freely. The output slot is read-only: it
 * shows a preview of whatever recipe currently matches and, on pickup, consumes
 * one ingredient from each grid cell before granting the result and firing
 * {@link SapientiaRecipeCompleteEvent}. All other slots (filler glass) reject clicks.
 */
public final class WorkbenchListener implements Listener {

    private final Plugin plugin;
    private final SapientiaRecipeRegistry recipes;
    private final UnlockService unlocks;
    private final ItemStack filler;

    public WorkbenchListener(
            @NotNull Plugin plugin,
            @NotNull SapientiaRecipeRegistry recipes,
            @NotNull UnlockService unlocks) {
        this.plugin = plugin;
        this.recipes = recipes;
        this.unlocks = unlocks;
        this.filler = buildFiller();
    }

    /** Opens a fresh workbench window for the player. */
    public void open(@NotNull Player player) {
        WorkbenchHolder holder = new WorkbenchHolder(Component.text("Sapientia Workbench"));
        Inventory inv = holder.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (!isGridSlot(i) && i != WorkbenchHolder.OUTPUT_SLOT) {
                inv.setItem(i, filler);
            }
        }
        player.openInventory(inv);
    }

    // ---------------------------------------------------------------------- clicks

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorkbenchHolder)) return;
        int slot = event.getRawSlot();

        // Clicks in the player inventory portion (raw >= 54) require no intervention.
        if (slot >= event.getInventory().getSize()) {
            if (event.getClick() == ClickType.DOUBLE_CLICK) event.setCancelled(true);
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Don't shift-click into filler / output slots.
                event.setCancelled(true);
            }
            return;
        }

        if (isGridSlot(slot)) {
            // Allow the click; refresh preview on the next tick once Bukkit has applied it.
            schedulePreviewRefresh(event.getInventory());
            return;
        }

        if (slot == WorkbenchHolder.OUTPUT_SLOT) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            ItemStack[] grid = extractGrid(event.getInventory());
            recipes.match(grid).ifPresent(recipe -> craft(player, event.getInventory(), recipe));
            return;
        }

        // Filler slots: never let the player take anything.
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorkbenchHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize() && !isGridSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        schedulePreviewRefresh(event.getInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorkbenchHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        for (int slot : WorkbenchHolder.GRID_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType().isAir()) continue;
            inv.clear(slot);
            player.getInventory().addItem(current.clone()).values().forEach(overflow ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }
        inv.clear(WorkbenchHolder.OUTPUT_SLOT);
    }

    // --------------------------------------------------------------------- helpers

    private void craft(Player player, Inventory inv, SapientiaRecipe recipe) {
        ItemStack result = recipes.effectiveResult(recipe);
        SapientiaRecipeCompleteEvent event = new SapientiaRecipeCompleteEvent(player, recipe, result);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        // Consume one item per non-empty grid slot (shaped, per-slot).
        for (int slot : WorkbenchHolder.GRID_SLOTS) {
            ItemStack in = inv.getItem(slot);
            if (in == null || in.getType().isAir()) continue;
            in.setAmount(in.getAmount() - 1);
            inv.setItem(slot, in.getAmount() <= 0 ? null : in);
        }

        player.getInventory().addItem(result).values().forEach(overflow ->
                player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        unlocks.unlock(player.getUniqueId(), recipe.id());
        schedulePreviewRefresh(inv);
    }

    private void schedulePreviewRefresh(Inventory inv) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshPreview(inv));
    }

    private void refreshPreview(Inventory inv) {
        ItemStack[] grid = extractGrid(inv);
        inv.setItem(WorkbenchHolder.OUTPUT_SLOT,
                recipes.match(grid).map(recipes::effectiveResult).orElse(null));
    }

    private ItemStack[] extractGrid(Inventory inv) {
        ItemStack[] out = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            out[i] = inv.getItem(WorkbenchHolder.GRID_SLOTS[i]);
        }
        return out;
    }

    private boolean isGridSlot(int slot) {
        for (int g : WorkbenchHolder.GRID_SLOTS) if (g == slot) return true;
        return false;
    }

    private static ItemStack buildFiller() {
        ItemStack s = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = s.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            s.setItemMeta(meta);
        }
        return s;
    }

    @SuppressWarnings("unused") // reserved for future extensions
    private static int[] gridSlots() {
        return Arrays.copyOf(WorkbenchHolder.GRID_SLOTS, 9);
    }
}
