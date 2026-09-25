package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after the server era changed (raised or lowered by the admin). */
public class SapientiaEraChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Era previous;
    private final Era current;

    public SapientiaEraChangeEvent(@NotNull Era previous, @NotNull Era current) {
        this.previous = previous;
        this.current = current;
    }

    public @NotNull Era previous() { return previous; }
    public @NotNull Era current() { return current; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
