package dev.brmz.sapientia.core.machine;

import java.util.HashMap;
import java.util.Map;

import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.engine.MachineBehavior;
import dev.brmz.sapientia.core.engine.MachineContext;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Lightable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The charcoal pit (era 1): a slow burn of 16 logs into 24 charcoal and 4 wood
 * ash. Like the recipe machines, the container directly above is the input and
 * the one directly below the output, so hoppers automate it.
 *
 * <p>One scheduler event per burn: a burn starts when 16 logs are waiting
 * (the pit lights up) and completes {@link #BURN_TICKS} later, when the logs
 * are taken and the products stored. If the logs are gone by then the burn is
 * lost; if the output is full the pit waits, lit, until there is room. Burns
 * in progress live in memory and restart after a server restart.
 */
public final class CharcoalPitProcessor {

    public static final NamespacedKey BLOCK_ID = NamespacedKey.fromString("sapientia:charcoal_pit");
    static final int LOGS = 16;
    static final int CHARCOAL = 24;
    static final int ASH = 4;
    static final int BURN_TICKS = 4 * 60 * 20;
    private static final int RETRY_TICKS = 40;

    private final ItemRegistry items;
    private final String ashId;
    /** Burns in progress: engine tick at which each one started. */
    private final Map<BlockKey, Long> burning = new HashMap<>();

    public CharcoalPitProcessor(@NotNull ItemRegistry items, @NotNull String ashId) {
        this.items = items;
        this.ashId = ashId;
    }

    public void register(@NotNull SapientiaEngine engine) {
        engine.registerBehavior(BLOCK_ID, RETRY_TICKS, context -> run(engine, context));
    }

    private int run(SapientiaEngine engine, MachineContext context) {
        BlockKey key = engine.keyOf(context);
        Block block = engine.blockOf(context);
        if (block == null) {
            burning.remove(key);
            return MachineBehavior.idle(RETRY_TICKS);
        }
        Long started = burning.get(key);
        if (started != null) {
            long left = started + BURN_TICKS - engine.currentTick();
            if (left > 0) return (int) left; // a burn from before the pit was reloaded
            int result = complete(block, key);
            if (result != 0) return result;
        }
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        if (input != null && inventoryAt(block.getRelative(0, -1, 0)) != null && countLogs(input) >= LOGS) {
            burning.put(key, engine.currentTick());
            setLit(block, true);
            return BURN_TICKS;
        }
        setLit(block, false);
        return MachineBehavior.idle(RETRY_TICKS);
    }

    /** Ends a burn; {@code 0} when done or lost, a retry delay while the output is full. */
    private int complete(Block block, BlockKey key) {
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        Inventory output = inventoryAt(block.getRelative(0, -1, 0));
        if (input == null || output == null || countLogs(input) < LOGS) {
            burning.remove(key);
            return 0;
        }
        ItemStack charcoal = new ItemStack(Material.CHARCOAL, CHARCOAL);
        ItemStack ash = items.createStack(ashId, ASH);
        if (!fits(output, ash == null ? new ItemStack[] {charcoal} : new ItemStack[] {charcoal, ash})) {
            return RETRY_TICKS;
        }
        takeLogs(input, LOGS);
        output.addItem(charcoal);
        if (ash != null) output.addItem(ash);
        burning.remove(key);
        return 0;
    }

    private int countLogs(Inventory inventory) {
        int count = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (isLog(stack)) count += stack.getAmount();
        }
        return count;
    }

    private void takeLogs(Inventory inventory, int amount) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && amount > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!isLog(stack)) continue;
            int taken = Math.min(amount, stack.getAmount());
            amount -= taken;
            stack.setAmount(stack.getAmount() - taken);
            inventory.setItem(slot, stack.getAmount() <= 0 ? null : stack);
        }
    }

    private boolean isLog(@Nullable ItemStack stack) {
        return stack != null && Tag.LOGS.isTagged(stack.getType()) && items.idOf(stack) == null;
    }

    /** Whether all the stacks fit together, tried on a copy of the inventory (once per burn). */
    private static boolean fits(Inventory inventory, ItemStack[] stacks) {
        ItemStack[] storage = inventory.getStorageContents();
        Inventory copy = org.bukkit.Bukkit.createInventory(null, ((storage.length + 8) / 9) * 9);
        for (int i = 0; i < storage.length; i++) {
            copy.setItem(i, storage[i] == null ? null : storage[i].clone());
        }
        for (int i = storage.length; i < copy.getSize(); i++) {
            copy.setItem(i, new ItemStack(Material.BARRIER, 64)); // padding slots never take items
        }
        for (ItemStack stack : stacks) {
            if (!copy.addItem(stack.clone()).isEmpty()) return false;
        }
        return true;
    }

    private static void setLit(Block block, boolean lit) {
        if (block.getBlockData() instanceof Lightable data && data.isLit() != lit) {
            data.setLit(lit);
            block.setBlockData(data, false);
        }
    }

    private static @Nullable Inventory inventoryAt(Block block) {
        BlockState state = block.getState(false);
        return state instanceof InventoryHolder holder ? holder.getInventory() : null;
    }
}
