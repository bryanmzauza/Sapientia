package dev.brmz.sapientia.core.logistics;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers to scan the 6-neighbourhood of a logistics node for an adjacent
 * vanilla {@link Container} (chest, barrel, hopper, dispenser, dropper,
 * shulker box, brewing stand, furnace, etc.) and to insert / extract items
 * from it. The producer / consumer logic intentionally mirrors the vanilla
 * hopper model (transfer up to N items per tick, first stackable slot first).
 *
 * <p>See ROADMAP 1.1.0.
 */
final class AdjacentContainers {

    private AdjacentContainers() {}

    private static final int[][] OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    /** Returns the first adjacent vanilla {@link Inventory}, or {@code null} if none. */
    static @Nullable Inventory findAdjacent(@NotNull Block origin) {
        for (int[] off : OFFSETS) {
            Block b = origin.getRelative(off[0], off[1], off[2]);
            BlockState state = b.getState(false);
            if (state instanceof Container container) {
                return container.getInventory();
            }
        }
        return null;
    }

    /**
     * Tries to insert {@code stack} into {@code inv}. Mutates {@code stack}'s
     * amount to whatever could not be placed; returns the number of items
     * actually inserted.
     */
    static int insertInto(@NotNull Inventory inv, @NotNull ItemStack stack) {
        int before = stack.getAmount();
        if (before <= 0) return 0;
        java.util.HashMap<Integer, ItemStack> overflow = inv.addItem(stack.clone());
        int leftover = 0;
        for (ItemStack remaining : overflow.values()) {
            leftover += remaining.getAmount();
        }
        int inserted = before - leftover;
        stack.setAmount(leftover);
        return inserted;
    }

    /**
     * Extracts up to {@code maxAmount} items from {@code inv}. Returns the
     * removed stack (amount may be smaller than requested), or {@code null}
     * when the inventory is empty.
     */
    static @Nullable ItemStack extractAny(@NotNull Inventory inv, int maxAmount) {
        if (maxAmount <= 0) return null;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack candidate = inv.getItem(slot);
            if (candidate == null || candidate.getType().isAir() || candidate.getAmount() <= 0) {
                continue;
            }
            int take = Math.min(maxAmount, candidate.getAmount());
            ItemStack out = candidate.clone();
            out.setAmount(take);
            candidate.setAmount(candidate.getAmount() - take);
            inv.setItem(slot, candidate.getAmount() <= 0 ? null : candidate);
            return out;
        }
        return null;
    }
}
