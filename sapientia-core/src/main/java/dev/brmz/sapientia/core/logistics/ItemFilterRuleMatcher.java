package dev.brmz.sapientia.core.logistics;

import java.util.List;

import dev.brmz.sapientia.api.logistics.ItemFilterMode;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluates an ordered {@link ItemFilterRule} list against an
 * {@link ItemStack}. Patterns supported:
 * <ul>
 *   <li>{@code namespace:id} — exact match (e.g. {@code minecraft:copper_ingot},
 *       {@code sapientia:wrench})</li>
 *   <li>{@code namespace:*} — namespace glob (e.g. {@code sapientia:*})</li>
 *   <li>{@code *} — full wildcard</li>
 * </ul>
 * Decision rules:
 * <ul>
 *   <li>Empty list → allow (open by default).</li>
 *   <li>Any matching {@link ItemFilterMode#BLACKLIST} rule → deny immediately.</li>
 *   <li>Otherwise: any whitelist rule present → must match at least one;
 *       no whitelist rules → allow.</li>
 * </ul>
 */
public final class ItemFilterRuleMatcher {

    private ItemFilterRuleMatcher() {}

    public static boolean allows(@NotNull List<ItemFilterRule> rules,
                                 @NotNull ItemRegistry registry,
                                 @NotNull ItemStack stack) {
        if (rules.isEmpty()) {
            return true;
        }
        String identity = identityOf(registry, stack);
        boolean hasWhitelist = false;
        boolean whitelistHit = false;
        for (ItemFilterRule rule : rules) {
            switch (rule.mode()) {
                case ACCEPT_ALL -> {
                    return true;
                }
                case BLACKLIST -> {
                    if (matches(rule.pattern(), identity)) {
                        return false;
                    }
                }
                case WHITELIST -> {
                    hasWhitelist = true;
                    if (matches(rule.pattern(), identity)) {
                        whitelistHit = true;
                    }
                }
            }
        }
        return !hasWhitelist || whitelistHit;
    }

    /** Returns the {@code namespace:id} identity string of a stack. */
    public static @NotNull String identityOf(@NotNull ItemRegistry registry, @NotNull ItemStack stack) {
        // Sapientia-tagged items take precedence over the underlying material.
        String saId = registry.idOf(stack);
        if (saId != null) {
            return saId;
        }
        return "minecraft:" + stack.getType().getKey().getKey();
    }

    private static boolean matches(@NotNull String pattern, @NotNull String identity) {
        String p = pattern.toLowerCase(java.util.Locale.ROOT).trim();
        String id = identity.toLowerCase(java.util.Locale.ROOT);
        if (p.equals("*")) return true;
        int star = p.indexOf('*');
        if (star < 0) {
            return p.equals(id);
        }
        // Support "namespace:*" or "prefix*" globs.
        String prefix = p.substring(0, star);
        return id.startsWith(prefix);
    }
}
