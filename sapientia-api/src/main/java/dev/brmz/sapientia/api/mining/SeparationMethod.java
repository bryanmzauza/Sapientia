package dev.brmz.sapientia.api.mining;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A way of separating minerals into elements, better era by era.
 *
 * @param id              method id, e.g. {@code sapientia:flotation}
 * @param era             era of the method
 * @param primaryYield    units of each primary element per fragment (the fraction is a chance)
 * @param secondaryChance chance of recovering one unit of each secondary element
 * @param traceChance     chance of recovering one unit of each trace element
 */
public record SeparationMethod(
        @NotNull NamespacedKey id,
        @NotNull Era era,
        double primaryYield,
        double secondaryChance,
        double traceChance) {

    public SeparationMethod {
        if (primaryYield < 0) throw new IllegalArgumentException("primaryYield must not be negative");
        if (secondaryChance < 0 || secondaryChance > 1) throw new IllegalArgumentException("secondaryChance must be 0..1");
        if (traceChance < 0 || traceChance > 1) throw new IllegalArgumentException("traceChance must be 0..1");
    }

    /** Whether the method recovers everything, so it never leaves tailings. */
    public boolean isComplete() {
        return secondaryChance >= 1 && traceChance >= 1;
    }
}
