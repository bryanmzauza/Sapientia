package dev.brmz.sapientia.api.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the logistics solver evaluates a stack against a {@code FILTER}
 * node's rules (T-300 / 1.1.0). Listeners may flip {@link #setAllowed(boolean)}
 * to override the rule decision; cancelling the event is equivalent to
 * forcing {@code allowed = false}.
 */
public class SapientiaItemFilterEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID filterNodeId;
    private final ItemStack stack;
    private boolean allowed;
    private boolean cancelled;

    public SapientiaItemFilterEvent(
            @NotNull UUID filterNodeId, @NotNull ItemStack stack, boolean allowedByRules) {
        this.filterNodeId = filterNodeId;
        this.stack = stack;
        this.allowed = allowedByRules;
    }

    public @NotNull UUID filterNodeId() {
        return filterNodeId;
    }

    /** Snapshot of the stack being evaluated. Mutations are not propagated. */
    public @NotNull ItemStack stack() {
        return stack;
    }

    public boolean isAllowed() {
        return allowed && !cancelled;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
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
