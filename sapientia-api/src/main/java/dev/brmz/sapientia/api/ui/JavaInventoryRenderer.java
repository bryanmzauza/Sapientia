package dev.brmz.sapientia.api.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a Java Edition inventory for a given UI context. Invoked by the Java UI
 * provider when a player opens a Sapientia UI. See docs/ui-strategy.md §2.
 *
 * <p>The provider owns inventory creation so it can install the correct
 * {@code InventoryHolder}. The renderer contributes the {@linkplain #title title},
 * {@linkplain #size size} and fills slots via {@link #render}.
 */
public interface JavaInventoryRenderer<C> {

    /** Inventory size in slots. Must be a multiple of 9 in {@code [9, 54]}. */
    int size(@NotNull Player player, @NotNull C context);

    /** Title shown on top of the inventory window. */
    default @NotNull Component title(@NotNull Player player, @NotNull C context) {
        return Component.text("Sapientia");
    }

    /** Fills the inventory created by the provider. Called once per open. */
    void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull C context);

    /** Invoked when the player clicks a slot. Default implementation is a no-op. */
    default void onClick(@NotNull Player player, @NotNull C context, int slot) {}

    /** Invoked when the inventory is closed. */
    default void onClose(@NotNull Player player, @NotNull C context) {}
}
