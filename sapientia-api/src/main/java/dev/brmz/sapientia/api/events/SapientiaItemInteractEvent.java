package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player right-clicks while holding a Sapientia item. Cancellable:
 * cancelling prevents the underlying vanilla interaction and any downstream
 * {@link SapientiaItem#onUse(SapientiaItemInteractEvent)} invocation.
 */
public class SapientiaItemInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack stack;
    private final SapientiaItem item;
    private final Action action;
    private final EquipmentSlot hand;
    private boolean cancelled;

    public SapientiaItemInteractEvent(
            @NotNull Player player,
            @NotNull ItemStack stack,
            @NotNull SapientiaItem item,
            @NotNull Action action,
            @NotNull EquipmentSlot hand) {
        this.player = player;
        this.stack = stack;
        this.item = item;
        this.action = action;
        this.hand = hand;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull ItemStack stack() {
        return stack;
    }

    public @NotNull SapientiaItem item() {
        return item;
    }

    public @NotNull Action action() {
        return action;
    }

    public @NotNull EquipmentSlot hand() {
        return hand;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
