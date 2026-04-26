package dev.brmz.sapientia.api.logistics;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * One row of a filter node's rule list (T-300 / 1.1.0).
 *
 * <p>The {@code pattern} matches against {@code material:namespacedKey} of an
 * incoming {@link ItemStack}. Supported forms:
 * <ul>
 *   <li>{@code minecraft:copper_ingot} — exact vanilla material</li>
 *   <li>{@code sapientia:wrench} — exact Sapientia item id</li>
 *   <li>{@code sapientia:*} — any Sapientia-tagged item (glob)</li>
 *   <li>{@code *} — wildcard (rarely useful; equivalent to {@link ItemFilterMode#ACCEPT_ALL})</li>
 * </ul>
 */
public record ItemFilterRule(int index, @NotNull ItemFilterMode mode, @NotNull String pattern) {

    public ItemFilterRule {
        if (index < 0) {
            throw new IllegalArgumentException("rule index must be >= 0");
        }
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("rule pattern must not be blank");
        }
    }
}
