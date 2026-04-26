package dev.brmz.sapientia.api.android;

import org.jetbrains.annotations.NotNull;

/**
 * One upgrade slot value: a {@link AndroidUpgradeKind} + tier in {@code [1, 4]}.
 * Records are immutable so they double as map keys / event payloads.
 *
 * <p>Used by {@link AndroidService} to expose the currently installed upgrades
 * of a given android. Effects are applied by the kinetic loop in 1.9.1; in
 * 1.9.0 the values are stored verbatim and surfaced through the
 * {@code SapientiaAndroidTickEvent}.
 */
public record AndroidUpgrade(@NotNull AndroidUpgradeKind kind, int tier) {

    public AndroidUpgrade {
        if (tier < 1 || tier > 4) {
            throw new IllegalArgumentException("tier must be in [1,4], got " + tier);
        }
    }
}
