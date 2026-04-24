package dev.brmz.sapientia.api.ui;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A UI addons can register once and have Sapientia render on whichever platform the
 * player is on. See docs/ui-strategy.md §1.2.
 */
public interface UIDescriptor<C> {

    /** Unique identifier for this UI. */
    @NotNull NamespacedKey key();

    /** Renderer for Java Edition. Required. */
    @NotNull JavaInventoryRenderer<C> javaRenderer();

    /**
     * Renderer for Bedrock Edition. When {@code null}, Sapientia falls back to a
     * generic Bedrock form generated from the Java inventory.
     */
    @Nullable BedrockFormRenderer<C> bedrockRenderer();
}
