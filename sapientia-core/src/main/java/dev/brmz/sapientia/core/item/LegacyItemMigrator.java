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

/**
 * Converts stacks of removed items to their replacements (same amount) when
 * players join, open a container or pick an item up. Each stack is converted
 * once; stacks nobody touches keep their old id until then, which is harmless
 * because nothing uses it any more.
 */
public final class LegacyItemMigrator implements Listener {

    private final ItemRegistry items;
    private final Map<String, String> replacements;

    /** @param replacements full removed id to full replacement id, e.g. {@code sapientia:tin_raw} */
    public LegacyItemMigrator(@NotNull ItemRegistry items, @NotNull Map<String, String> replacements) {
        this.items = items;
        this.replacements = Map.copyOf(replacements);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        convert(player.getInventory());
        convert(player.getEnderChest());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(@NotNull InventoryOpenEvent event) {
        convert(event.getInventory());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack replaced = replacement(item.getItemStack());
        if (replaced != null) item.setItemStack(replaced);
    }

    /** Converts every legacy stack in {@code inventory}; returns how many stacks changed. */
    public int convert(@NotNull Inventory inventory) {
        int changed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack replaced = replacement(contents[slot]);
            if (replaced != null) {
                inventory.setItem(slot, replaced);
                changed++;
            }
        }
        return changed;
    }

    private ItemStack replacement(ItemStack stack) {
        String id = items.idOf(stack);
        if (id == null) return null;
        String target = replacements.get(id);
        return target == null ? null : items.createStack(target, stack.getAmount());
    }
}
