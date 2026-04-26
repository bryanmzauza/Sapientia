package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.logistics.ItemNode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the logistics solver chooses a {@code PRODUCER → CONSUMER} route
 * for a batch of items (T-300 / 1.1.0). Read-only. Listeners may use this to
 * track flows for telemetry / quests.
 */
public class SapientiaItemRouteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemNode from;
    private final ItemNode to;
    private final ItemStack stack;
    private final int amount;

    public SapientiaItemRouteEvent(
            @NotNull ItemNode from, @NotNull ItemNode to,
            @NotNull ItemStack stack, int amount) {
        this.from = from;
        this.to = to;
        this.stack = stack;
        this.amount = amount;
    }

    public @NotNull ItemNode from() {
        return from;
    }

    public @NotNull ItemNode to() {
        return to;
    }

    /** Stack being routed (single batch, snapshot — do not mutate). */
    public @NotNull ItemStack stack() {
        return stack;
    }

    public int amount() {
        return amount;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
