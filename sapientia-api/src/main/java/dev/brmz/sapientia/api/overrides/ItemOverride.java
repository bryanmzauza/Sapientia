package dev.brmz.sapientia.api.overrides;

import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Operator-authored tweak applied over a registered {@code SapientiaItem}
 * (ADR-012, T-160 / 0.5.0).
 *
 * <p>Content is always Java-defined; YAML files only rebalance existing
 * entries. Every field except {@link #id()} is optional &mdash; missing fields
 * keep the original Java value.
 */
public record ItemOverride(
        @NotNull NamespacedKey id,
        @NotNull Optional<Material> material,
        @NotNull Optional<String> displayNameKey,
        @NotNull Optional<List<String>> loreKeys,
        @NotNull Optional<Integer> customModelData) {

    public ItemOverride {
        if (loreKeys.isPresent()) {
            loreKeys = Optional.of(List.copyOf(loreKeys.get()));
        }
    }
}
