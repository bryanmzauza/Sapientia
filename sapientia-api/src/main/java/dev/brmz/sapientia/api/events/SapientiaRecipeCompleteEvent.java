package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired right before a player pulls the result out of a Sapientia workbench
 * (T-132 / 0.4.0). Cancelling prevents the craft: the grid is not consumed and
 * no output is given.
 */
public class SapientiaRecipeCompleteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SapientiaRecipe recipe;
    private final ItemStack result;
    private boolean cancelled;

    public SapientiaRecipeCompleteEvent(
            @NotNull Player player,
            @NotNull SapientiaRecipe recipe,
            @NotNull ItemStack result) {
        this.player = player;
        this.recipe = recipe;
        this.result = result;
    }

    public @NotNull Player player() { return player; }
    public @NotNull SapientiaRecipe recipe() { return recipe; }
    public @NotNull ItemStack result() { return result; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
