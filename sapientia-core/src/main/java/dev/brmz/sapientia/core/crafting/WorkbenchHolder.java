package dev.brmz.sapientia.core.crafting;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom {@link InventoryHolder} marking a Sapientia workbench window (T-130 / 0.4.0).
 * The layout is a 6-row (54 slot) chest inventory with the 3&times;3 crafting grid in
 * the middle and a single preview/output slot to its right. All other slots are
 * filled with filler glass and rejected by {@link WorkbenchListener}.
 */
public final class WorkbenchHolder implements InventoryHolder {

    /** 9 slots of the 3x3 grid, row-major (top-left → bottom-right). */
    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    /** Preview / take-output slot. */
    public static final int OUTPUT_SLOT = 24;

    private final Inventory inventory;

    public WorkbenchHolder(@NotNull Component title) {
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
