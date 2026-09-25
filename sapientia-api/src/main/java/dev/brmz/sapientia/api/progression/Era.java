package dev.brmz.sapientia.api.progression;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The 25 eras of Sapientia, from the arrival to the singularity. The server
 * admin unlocks eras in order; content of a locked era cannot be crafted,
 * placed, processed or found in the world.
 *
 * <p>Era 0 is always unlocked. Each era names a gateway item: making it is the
 * player's goal for that era.
 */
public enum Era {
    ARRIVAL("workbench"),
    STONE_AGE("clay_furnace"),
    COPPER_AGE("copper_hammer"),
    BRONZE_AGE("bronze_ingot"),
    IRON_AGE("iron_bloom"),
    CLASSICAL_ANTIQUITY("water_wheel"),
    MIDDLE_AGES("blast_furnace_controller"),
    RENAISSANCE("precision_mechanism"),
    INDUSTRIAL_REVOLUTION("boiler"),
    ELECTRICITY("machine_casing"),
    STEEL_AGE("stainless_steel_ingot"),
    OIL_AND_CHEMISTRY("plastic"),
    ELECTRONICS("processor_t1"),
    ATOMIC_AGE("fuel_rod"),
    SPACE_AGE("satellite"),
    INFORMATION_AGE("computer"),
    ROBOTICS("ai_core"),
    RENEWABLE_ENERGY("solar_cell_t3"),
    FUSION("superconducting_coil"),
    NANOTECHNOLOGY("molecular_assembler"),
    ORBITAL_AGE("orbital_module"),
    QUANTUM_AGE("quantum_processor"),
    ANTIMATTER("antimatter_cell"),
    INTERSTELLAR("warp_core"),
    SINGULARITY(null);

    private static final Era[] VALUES = values();

    private final @Nullable String gatewayItem;

    Era(@Nullable String gatewayItem) {
        this.gatewayItem = gatewayItem;
    }

    /** Era number, 0 to 24. */
    public int number() {
        return ordinal();
    }

    /** Era with the given number, clamped to 0..24. */
    public static @NotNull Era of(int number) {
        return VALUES[Math.max(0, Math.min(VALUES.length - 1, number))];
    }

    /** The first era. */
    public static @NotNull Era first() {
        return VALUES[0];
    }

    /** The last era. */
    public static @NotNull Era last() {
        return VALUES[VALUES.length - 1];
    }

    /** The following era, or {@code null} for the last one. */
    public @Nullable Era next() {
        return ordinal() + 1 < VALUES.length ? VALUES[ordinal() + 1] : null;
    }

    /** Whether this era comes after {@code other}. */
    public boolean isAfter(@NotNull Era other) {
        return ordinal() > other.ordinal();
    }

    /** i18n key of the era's name, e.g. {@code era.stone_age.name}. */
    public @NotNull String nameKey() {
        return "era." + name().toLowerCase(Locale.ROOT) + ".name";
    }

    /** i18n key of a one-line summary of what the era brings. */
    public @NotNull String summaryKey() {
        return "era." + name().toLowerCase(Locale.ROOT) + ".summary";
    }

    /** Id of the item that marks the era as completed, or {@code null} for the last era. */
    public @Nullable NamespacedKey gatewayItem() {
        return gatewayItem == null ? null : new NamespacedKey("sapientia", gatewayItem);
    }
}
