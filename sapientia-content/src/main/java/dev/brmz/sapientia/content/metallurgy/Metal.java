package dev.brmz.sapientia.content.metallurgy;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Catalog of metals shipped in Sapientia 1.4.0 (T-402 / T-403).
 *
 * <p>The first six are <em>raw metals</em> obtained from world-ore mining.
 * The remaining three are <em>alloys</em> produced via the mixer or the
 * induction-furnace multiblock and therefore <em>do not</em> register a
 * {@link MetalForm#RAW} form.
 */
public enum Metal {
    // --- Raw metals (T-402 / 1.4.0) ---
    COPPER  (false, "Copper",   "Cobre"),
    TIN     (false, "Tin",      "Estanho"),
    ZINC    (false, "Zinc",     "Zinco"),
    LEAD    (false, "Lead",     "Chumbo"),
    SILVER  (false, "Silver",   "Prata"),
    NICKEL  (false, "Nickel",   "Níquel"),

    // --- Raw metals (T-421 / 1.6.0) ---
    ALUMINUM(false, "Aluminum", "Alumínio"),
    SILICON (false, "Silicon",  "Silício"),
    TITANIUM(false, "Titanium", "Titânio"),
    LITHIUM (false, "Lithium",  "Lítio"),

    // --- Alloys (T-403 / 1.4.0) ---
    BRONZE  (true,  "Bronze",   "Bronze"),
    BRASS   (true,  "Brass",    "Latão"),
    ELECTRUM(true,  "Electrum", "Eletro"),

    // --- Alloys (T-424 / 1.6.0 — HV tier) ---
    STAINLESS_STEEL(true, "Stainless Steel", "Aço Inoxidável"),
    DAMASCUS_STEEL (true, "Damascus Steel",  "Aço Damasco"),
    NICHROME       (true, "Nichrome",        "Nicromo");

    private final boolean alloy;
    private final String displayEn;
    private final String displayPt;

    Metal(boolean alloy, @NotNull String displayEn, @NotNull String displayPt) {
        this.alloy = alloy;
        this.displayEn = displayEn;
        this.displayPt = displayPt;
    }

    public boolean isAlloy() {
        return alloy;
    }

    /** Snake-case base id (e.g. {@code copper}). */
    public @NotNull String idBase() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @NotNull String displayEn() {
        return displayEn;
    }

    public @NotNull String displayPt() {
        return displayPt;
    }

    /** Forms registered for this metal. Alloys skip {@link MetalForm#RAW}. */
    public @NotNull Set<MetalForm> forms() {
        if (!alloy) return EnumSet.allOf(MetalForm.class);
        Set<MetalForm> out = EnumSet.allOf(MetalForm.class);
        out.remove(MetalForm.RAW);
        return out;
    }
}
