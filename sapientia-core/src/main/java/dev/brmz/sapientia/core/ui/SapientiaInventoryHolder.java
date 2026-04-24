package dev.brmz.sapientia.core.ui;

import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Binds a Sapientia UI context to the inventory the player has open so the
 * {@link UIService} listeners can route events back to the descriptor's renderer.
 */
final class SapientiaInventoryHolder<C> implements InventoryHolder {

    private final JavaInventoryRenderer<C> renderer;
    private final C context;
    private Inventory inventory;

    SapientiaInventoryHolder(@NotNull JavaInventoryRenderer<C> renderer, @NotNull C context) {
        this.renderer = renderer;
        this.context = context;
    }

    void bind(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    void dispatchClick(@NotNull Player player, int slot) {
        renderer.onClick(player, context, slot);
    }

    void dispatchClose(@NotNull Player player) {
        renderer.onClose(player, context);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
