package dev.brmz.sapientia.content.metallurgy;

import java.util.Locale;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * The seven processed forms a metal can take in Sapientia (T-402 / 1.4.0).
 *
 * <p>Each form has a default vanilla {@link Material} used as the visual base.
 * Concrete metals may override per-metal materials when needed, but most forms
 * fall back to the generic placeholders defined here.
 */
public enum MetalForm {
    /** Raw chunk (drop from world ore — pre-smelt). */
    RAW("raw", Material.RAW_COPPER),
    /** Powder produced by macerator. */
    DUST("dust", Material.SUGAR),
    /** Smelted ingot. */
    INGOT("ingot", Material.IRON_INGOT),
    /** Compressed 9-ingot storage block. */
    BLOCK("block", Material.IRON_BLOCK),
    /** Rolled plate — output of plate press. */
    PLATE("plate", Material.PAPER),
    /** Drawn wire — output of extractor. */
    WIRE("wire", Material.STRING),
    /** Lathed rod — input for gears. */
    ROD("rod", Material.BLAZE_ROD),
    /** Toothed gear — output of bench saw. */
    GEAR("gear", Material.CLOCK),
    /** Tiny screw — output of bench saw. */
    SCREW("screw", Material.NETHER_STAR);

    private final String suffix;
    private final Material defaultMaterial;

    MetalForm(@NotNull String suffix, @NotNull Material defaultMaterial) {
        this.suffix = suffix;
        this.defaultMaterial = defaultMaterial;
    }

    /** Snake-case suffix appended to the metal id (e.g. {@code copper_dust}). */
    public @NotNull String suffix() {
        return suffix;
    }

    /** Default vanilla material backing this form. */
    public @NotNull Material defaultMaterial() {
        return defaultMaterial;
    }

    /** Lower-case display name (e.g. {@code "dust"}). */
    public @NotNull String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
