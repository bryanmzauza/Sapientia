package dev.brmz.sapientia.content.mining;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Items removed in 2.0.1 and what existing stacks of them become. The raw
 * metal items were replaced by mineral fragments; silicon has no mineral and
 * becomes silicon dust.
 */
public final class LegacyItemIds {

    private static final Map<String, String> REPLACEMENTS = Map.of(
            "copper_raw", "native_copper_fragment",
            "tin_raw", "cassiterite_fragment",
            "zinc_raw", "sphalerite_fragment",
            "lead_raw", "galena_fragment",
            "silver_raw", "native_silver_fragment",
            "nickel_raw", "pentlandite_fragment",
            "aluminum_raw", "bauxite_fragment",
            "silicon_raw", "silicon_dust",
            "titanium_raw", "ilmenite_fragment",
            "lithium_raw", "spodumene_fragment");

    private LegacyItemIds() {}

    /** Removed item id (without namespace) to its replacement (without namespace). */
    public static @NotNull Map<String, String> replacements() {
        return REPLACEMENTS;
    }
}
