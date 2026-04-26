package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player right-clicks a Sapientia block. The Core automatically
 * cancels the underlying vanilla interaction (chest opens, crafting tables, etc.)
 * before dispatching this event.
 */
public class SapientiaBlockInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final SapientiaBlock definition;
    private final Action action;
    private final EquipmentSlot hand;
    private boolean cancelled;

    public SapientiaBlockInteractEvent(
            @NotNull Player player,
            @NotNull Block block,
            @NotNull SapientiaBlock definition,
            @NotNull Action action,
            @NotNull EquipmentSlot hand) {
        this.player = player;
        this.block = block;
        this.definition = definition;
        this.action = action;
        this.hand = hand;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull Block block() {
        return block;
    }

    public @NotNull SapientiaBlock definition() {
        return definition;
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
