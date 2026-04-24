package dev.brmz.sapientia.api.ui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a Bedrock Edition form for a given UI context. Invoked by the Bedrock UI
 * provider (backed by Floodgate) when a Bedrock player opens a Sapientia UI.
 * See docs/ui-strategy.md §3.
 */
@FunctionalInterface
public interface BedrockFormRenderer<C> {

    /**
     * Sends the form to the player. The implementation is responsible for constructing
     * and dispatching the Floodgate form directly.
     */
    void open(@NotNull Player player, @NotNull C context);
}
