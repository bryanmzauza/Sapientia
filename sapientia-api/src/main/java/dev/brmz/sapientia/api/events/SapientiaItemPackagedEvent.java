package dev.brmz.sapientia.api.events;

import dev.brmz.sapientia.api.logistics.ItemNode;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired when a {@code packager} block bundles items into a packaged_bundle
 * stack (T-441 / T-450 / 1.8.0). Cancellable: addons may veto the bundle
 * (e.g. to enforce a recipe template or block soulbound items).
 *
 * <p>1.8.0 ships the event scaffolding; the kinetic packaging tick lands with
 * the routing rework in 1.8.1.
 */
public class SapientiaItemPackagedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemNode packager;
    private final List<ItemStack> contents;
    private final ItemStack bundle;
    private boolean cancelled;

    public SapientiaItemPackagedEvent(@NotNull ItemNode packager,
                                      @NotNull List<ItemStack> contents,
                                      @NotNull ItemStack bundle) {
        this.packager = packager;
        this.contents = List.copyOf(contents);
        this.bundle = bundle;
    }

    /** The packager logistics node that produced the bundle. */
    public @NotNull ItemNode packager() {
        return packager;
    }

    /** Snapshot of the input stacks consumed to form the bundle. Immutable. */
    public @NotNull List<ItemStack> contents() {
        return contents;
    }

    /** The resulting bundle stack about to be inserted into the network. */
    public @NotNull ItemStack bundle() {
        return bundle;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
