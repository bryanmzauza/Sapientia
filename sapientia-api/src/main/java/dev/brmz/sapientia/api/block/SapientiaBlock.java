package dev.brmz.sapientia.api.block;

import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A fixed, Java-defined block contributed to the Sapientia catalog (see ADR-012).
 *
 * <p>Each block declares a stable {@link #id()}, the vanilla {@link Material} that
 * appears in the world when placed, and the id of the item form that places it
 * (usually equal to {@link #id()}). Behavior hooks default to no-ops.
 */
public interface SapientiaBlock {

    /** Stable registry id, e.g. {@code sapientia:pedestal}. */
    @NotNull NamespacedKey id();

    /** Registry id of the item form used to place this block. Defaults to {@link #id()}. */
    default @NotNull NamespacedKey itemId() {
        return id();
    }

    /** Vanilla material rendered in the world for this block. */
    @NotNull Material baseMaterial();

    /** i18n key that resolves to the display name of this block and its item form. */
    @NotNull String displayNameKey();

    /**
     * Whether this block participates in the {@code TickBucketing} dispatcher.
     * Defaults to {@code false}. Override and return {@code true} for machines or
     * anything that requires periodic work.
     */
    default boolean ticks() {
        return false;
    }

    /** Guide category this block is listed under. Defaults to {@link GuideCategory#MISC}. */
    default @NotNull GuideCategory guideCategory() {
        return GuideCategory.MISC;
    }

    /** Whether this block appears in the guide before being unlocked. Defaults to {@code true}. */
    default boolean discoveredByDefault() {
        return true;
    }

    /** Invoked after the block was successfully placed and persisted. */
    default void onPlace(@NotNull SapientiaBlockPlaceEvent event) {}

    /** Invoked before the block is removed; cancel the event to prevent break. */
    default void onBreak(@NotNull SapientiaBlockBreakEvent event) {}

    /** Invoked when a player right-clicks the block. Defaults to no-op. */
    default void onInteract(@NotNull SapientiaBlockInteractEvent event) {}
}
