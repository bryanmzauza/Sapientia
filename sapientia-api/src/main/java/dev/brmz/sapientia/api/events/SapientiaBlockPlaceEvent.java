package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after the Core has identified a vanilla {@code BlockPlaceEvent} as placing
 * a Sapientia block, but before persistence and the block's {@code onPlace} hook
 * run. Cancelling rolls back the placement.
 */
public class SapientiaBlockPlaceEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final SapientiaBlock definition;
    private final ItemStack placedStack;
    private boolean cancelled;

    public SapientiaBlockPlaceEvent(
            @NotNull Player player,
            @NotNull Block block,
            @NotNull SapientiaBlock definition,
            @NotNull ItemStack placedStack) {
        this.player = player;
        this.block = block;
        this.definition = definition;
        this.placedStack = placedStack;
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

    public @NotNull ItemStack placedStack() {
        return placedStack;
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
