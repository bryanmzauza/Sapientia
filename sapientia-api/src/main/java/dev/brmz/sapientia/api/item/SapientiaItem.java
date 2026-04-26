package dev.brmz.sapientia.api.item;

import java.util.List;

import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A fixed, Java-defined item contributed to the Sapientia catalog (see ADR-012).
 *
 * <p>Implementations declare a stable {@link #id() id}, the vanilla {@link Material}
 * that backs their stack, and the i18n keys used to render their display name and lore.
 * Behavior is implemented by overriding {@link #onUse(SapientiaItemInteractEvent)}.
 */
public interface SapientiaItem {

    /** Stable registry id, e.g. {@code sapientia:wrench}. */
    @NotNull NamespacedKey id();

    /** Vanilla material used as the visual base for this item's stacks. */
    @NotNull Material baseMaterial();

    /** i18n key that resolves to the display name (rendered by the Core). */
    @NotNull String displayNameKey();

    /** Ordered list of i18n keys used as lore lines. Empty by default. */
    default @NotNull List<String> loreKeys() {
        return List.of();
    }

    /** Guide category this item is listed under. Defaults to {@link GuideCategory#MISC}. */
    default @NotNull GuideCategory guideCategory() {
        return GuideCategory.MISC;
    }

    /** Whether this item appears in the guide before being unlocked. Defaults to {@code true}. */
    default boolean discoveredByDefault() {
        return true;
    }

    /**
     * Custom-model-data tag applied to the rendered {@link org.bukkit.inventory.ItemStack}.
     * A value of {@code 0} (the default) leaves the stack untagged and reuses the vanilla
     * model. Resource packs and Geyser mappings consume this number to pick the override
     * model for both Java and Bedrock.
     */
    default int customModelData() {
        return 0;
    }

    /**
     * Invoked when a player right-clicks holding a stack of this item.
     * The default implementation is a no-op; override to react.
     */
    default void onUse(@NotNull SapientiaItemInteractEvent event) {}
}
