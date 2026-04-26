package dev.brmz.sapientia.api.android;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

/**
 * The four upgrade categories an android accepts (T-454 / 1.9.0).
 *
 * <ul>
 *   <li>{@link #AI_CHIP} — scales the work radius (T1..T4 ⇒ 4 / 6 / 8 / 12 blocks).</li>
 *   <li>{@link #MOTOR} — scales instructions per tick. Capped at 1/tick in 1.9.0
 *       per T-451 (every android executes at most one logic instruction per
 *       game tick); higher tiers unlock as the budget envelope is validated
 *       in 1.9.1.</li>
 *   <li>{@link #ARMOUR} — scales HP (T1..T4 ⇒ 100 / 200 / 400 / 800).</li>
 *   <li>{@link #FUEL_MODULE} — scales biofuel ↔ SU conversion ratio
 *       (T1..T4 ⇒ 1 / 2 / 4 / 8 SU per biofuel unit).</li>
 * </ul>
 *
 * <p>Each kind ships in four tiers (1..4); see {@link AndroidUpgrade}.
 */
public enum AndroidUpgradeKind {
    AI_CHIP     ("ai_chip"),
    MOTOR       ("motor_chip"),
    ARMOUR      ("armour_plate"),
    FUEL_MODULE ("fuel_module");

    private final String idBase;

    AndroidUpgradeKind(@NotNull String idBase) {
        this.idBase = idBase;
    }

    public @NotNull String idBase() {
        return idBase;
    }

    public @NotNull String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
