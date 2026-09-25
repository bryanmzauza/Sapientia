package dev.brmz.sapientia.core.android;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers for reading / writing the inventory chests adjacent to an android
 * (T-451 / 1.9.1).
 *
 * <p>Convention follows the existing {@code MachineProcessor}:
 * <ul>
 *   <li>{@code +Y} (block above) is the input — fuel and trader supplies.</li>
 *   <li>{@code -Y} (block below) is the output — generated loot, exchange
 *       proceeds.</li>
 * </ul>
 *
 * <p>Vanilla containers (chest, barrel, hopper, dispenser, dropper, shulker
 * box) and any {@link InventoryHolder} are accepted; non-container blocks
 * (or empty world space) silently make the action a no-op for that tick,
 * preserving the deterministic invariant "no work without input/output".
 */
final class AndroidIO {

    /**
     * Conversion table for solid fuel inserted into the input chest. The
     * values are mb-equivalent for the {@link AndroidUpgradeScaling} buffer
     * (which is sized in mb). Operators can rebalance later via YAML
     * overrides if the catalogue tuning needs nudging.
     */
    static final Map<Material, Long> SOLID_FUEL = Map.of(
            Material.COAL,        1_000L,
            Material.CHARCOAL,      800L,
            Material.BLAZE_POWDER, 4_000L,
            Material.BLAZE_ROD,    8_000L);

    private AndroidIO() {}

    static @Nullable Inventory inputAbove(@NotNull Block androidBlock) {
        return inventoryAt(androidBlock.getRelative(0, 1, 0));
    }

    static @Nullable Inventory outputBelow(@NotNull Block androidBlock) {
        return inventoryAt(androidBlock.getRelative(0, -1, 0));
    }

    static @Nullable Inventory inventoryAt(@NotNull Block block) {
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }

    /**
     * Pulls one fuel item from the input chest, returning the mb of fuel
     * deposited into the buffer. Returns 0 when no eligible fuel is found.
     */
    static long pullFuelInto(@NotNull SimpleAndroidNode node, @NotNull Inventory input) {
        long capacity = AndroidUpgradeScaling.fuelBufferMax(node.fuelTier());
        long current = node.fuelBuffer();
        if (current >= capacity) return 0L;
        for (int slot = 0; slot < input.getSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack == null || stack.getAmount() <= 0) continue;
            Long perUnit = SOLID_FUEL.get(stack.getType());
            if (perUnit == null) continue;
            long deposit = Math.min(perUnit, capacity - current);
            if (deposit <= 0) return 0L;
            node.setFuelBuffer(current + deposit);
            stack.setAmount(stack.getAmount() - 1);
            if (stack.getAmount() <= 0) input.setItem(slot, null);
            return deposit;
        }
        return 0L;
    }

    /**
     * Tries to deposit a stack into the output chest. Returns {@code true}
     * on full success — partial inserts roll back so loot is not silently
     * dropped on the floor.
     */
    static boolean depositOrReject(@NotNull Inventory output, @NotNull ItemStack stack) {
        ItemStack toAdd = stack.clone();
        Map<Integer, ItemStack> rejected = output.addItem(toAdd);
        return rejected.isEmpty();
    }

    /**
     * Pulls exactly {@code amount} of any single item from the input chest
     * (first match wins). Returns the consumed stack, or {@code null} when
     * no slot has at least {@code amount} of the same material. Used by the
     * trader and builder behaviours.
     */
    static @Nullable ItemStack consumeAny(@NotNull Inventory input, int amount) {
        for (int slot = 0; slot < input.getSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack == null || stack.getAmount() < amount) continue;
            ItemStack taken = stack.clone();
            taken.setAmount(amount);
            stack.setAmount(stack.getAmount() - amount);
            if (stack.getAmount() <= 0) input.setItem(slot, null);
            return taken;
        }
        return null;
    }
}
