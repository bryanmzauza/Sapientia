package dev.brmz.sapientia.api.android;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

/**
 * The eight android archetypes shipped in milestone 1.9.0 (T-452).
 *
 * <p>1.9.0 ships the catalogue: each type is a placeable, persistent
 * {@code SapientiaBlock} that registers as an LV energy {@code CONSUMER}
 * and accepts an assigned logic program. The live AI behaviour (crop / log
 * scan, slayer melee policy, trader exchange tables, ...) lands with the
 * kinetic loop in 1.9.1 — see ROADMAP 1.9.0 and docs/content-spec-T-45x.md.
 *
 * <p>The {@link #idBase()} maps directly to the registry id
 * (e.g. {@code sapientia:android_farmer}) and to the i18n key
 * {@code block.android_<idBase>.name}.
 */
public enum AndroidType {
    FARMER     ("android_farmer"),
    LUMBERJACK ("android_lumberjack"),
    MINER      ("android_miner"),
    FISHERMAN  ("android_fisherman"),
    BUTCHER    ("android_butcher"),
    BUILDER    ("android_builder"),
    SLAYER     ("android_slayer"),
    TRADER     ("android_trader");

    private final String idBase;

    AndroidType(@NotNull String idBase) {
        this.idBase = idBase;
    }

    public @NotNull String idBase() {
        return idBase;
    }

    public @NotNull String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
