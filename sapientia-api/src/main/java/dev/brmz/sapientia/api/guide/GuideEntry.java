package dev.brmz.sapientia.api.guide;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * An entry listed in the in-game guide (T-150 / 0.4.0). Usually derived by the
 * core from registered items and blocks, but addons can also contribute ad-hoc
 * entries via {@link GuideService#register(GuideEntry)}.
 */
public record GuideEntry(
        @NotNull NamespacedKey id,
        @NotNull GuideCategory category,
        @NotNull String displayNameKey,
        @NotNull Material icon,
        boolean discoveredByDefault) {

    public GuideEntry {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (category == null) throw new IllegalArgumentException("category must not be null");
        if (displayNameKey == null || displayNameKey.isBlank()) {
            throw new IllegalArgumentException("displayNameKey must not be blank");
        }
        if (icon == null) throw new IllegalArgumentException("icon must not be null");
    }
}
