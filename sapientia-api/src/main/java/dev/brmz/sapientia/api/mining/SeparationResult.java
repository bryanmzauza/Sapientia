package dev.brmz.sapientia.api.mining;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Outcome of separating one fragment (or one tailings item).
 *
 * @param elements units recovered per element id
 * @param tailings whether a tailings item is left for reprocessing
 */
public record SeparationResult(@NotNull Map<String, Integer> elements, boolean tailings) {

    public SeparationResult {
        elements = Map.copyOf(elements);
    }
}
