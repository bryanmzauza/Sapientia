package dev.brmz.sapientia.api.overrides;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Operator-authored tweak applied over a registered {@code SapientiaBlock}
 * (T-160 / 0.5.0). Only the item form's stack is affected &mdash; world blocks
 * already placed keep their vanilla state until replaced.
 */
public record BlockOverride(
        @NotNull NamespacedKey id,
        @NotNull Optional<Material> material,
        @NotNull Optional<String> displayNameKey) {
}
