package dev.brmz.sapientia.api.events;

import java.util.List;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player discovered a Sapientia item for the first time, with
 * the workbench recipes that this discovery unlocked for them.
 */
public class SapientiaDiscoveryEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final NamespacedKey item;
    private final List<NamespacedKey> unlockedRecipes;

    public SapientiaDiscoveryEvent(@NotNull UUID player, @NotNull NamespacedKey item,
                                   @NotNull List<NamespacedKey> unlockedRecipes) {
        this.player = player;
        this.item = item;
        this.unlockedRecipes = List.copyOf(unlockedRecipes);
    }

    public @NotNull UUID player() { return player; }
    public @NotNull NamespacedKey item() { return item; }
    public @NotNull List<NamespacedKey> unlockedRecipes() { return unlockedRecipes; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
