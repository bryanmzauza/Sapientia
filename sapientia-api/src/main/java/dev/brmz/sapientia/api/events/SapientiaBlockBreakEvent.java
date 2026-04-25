package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a Sapientia block is removed from the world. Cancelling preserves
 * the block (both the world state and the persisted row).
 */
public class SapientiaBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final SapientiaBlock definition;
    private boolean dropItem = true;
    private boolean cancelled;

    public SapientiaBlockBreakEvent(
            @NotNull Player player,
            @NotNull Block block,
            @NotNull SapientiaBlock definition) {
        this.player = player;
        this.block = block;
        this.definition = definition;
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

    /** Whether the Core should drop the block's item form at the break location. */
    public boolean dropItem() {
        return dropItem;
    }

    public void dropItem(boolean drop) {
        this.dropItem = drop;
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
