package dev.brmz.sapientia.api.fluids;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * A registered fluid type (T-301 / 1.2.0). Java-declared per ADR-012; addons
 * register additional fluids via {@link FluidService#registerType(FluidType)}.
 *
 * <p>The {@link #density} value is informational metadata used by the upcoming
 * gravity / vertical-flow pass; the 1.2.0 solver treats every fluid as a
 * pressurised volume in millibuckets (mB).
 */
public record FluidType(
        @NotNull NamespacedKey id,
        @NotNull String displayKey,
        int color,
        int density,
        boolean hot) {

    public FluidType {
        if (id == null) throw new IllegalArgumentException("id");
        if (displayKey == null || displayKey.isBlank()) {
            throw new IllegalArgumentException("displayKey");
        }
    }
}
