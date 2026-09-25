package dev.brmz.sapientia.core.item;

import java.util.Map;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps existing stacks current when players join, open a container or pick an
 * item up: stacks of removed items become their replacements (same amount),
 * and stacks made by an older version get the current name, description (with
 * the era line) and texture model. Each stack is updated once; the check is a
 * tag lookup for stacks that are already current.
 */
public final class ItemRefresher implements Listener {

    private final ItemRegistry items;
    private final Map<String, String> replacements;

    /** @param replacements full removed id to full replacement id, e.g. {@code sapientia:tin_raw} */
    public ItemRefresher(@NotNull ItemRegistry items, @NotNull Map<String, String> replacements) {
        this.items = items;
        this.replacements = Map.copyOf(replacements);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        update(player.getInventory());
        update(player.getEnderChest());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(@NotNull InventoryOpenEvent event) {
        update(event.getInventory());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack updated = updated(item.getItemStack());
        if (updated != null) item.setItemStack(updated);
    }

    /** Updates every outdated stack in {@code inventory}; returns how many stacks changed. */
    public int update(@NotNull Inventory inventory) {
        int changed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack updated = updated(contents[slot]);
            if (updated != null) {
                inventory.setItem(slot, updated);
                changed++;
            }
        }
        return changed;
    }

    /** The updated stack, or {@code null} when it is already current. */
    private @Nullable ItemStack updated(@Nullable ItemStack stack) {
        String id = items.idOf(stack);
        if (id == null) return null;
        String target = replacements.get(id);
        if (target != null) {
            return items.createStack(target, stack.getAmount());
        }
        ItemStack copy = stack.clone();
        return items.refresh(copy) ? copy : null;
    }
}
