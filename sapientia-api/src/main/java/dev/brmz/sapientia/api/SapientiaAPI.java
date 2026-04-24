package dev.brmz.sapientia.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level Sapientia service. Additional services (machines, energy, UI) are exposed as
 * sub-services and will be added as the core grows. See docs/api-spec.md.
 */
public interface SapientiaAPI {

    /** Version of the API currently loaded. */
    @NotNull Version version();

    /** Detected platform of a player. Never null; unknown players default to JAVA. */
    @NotNull PlatformType platformOf(@NotNull Player player);

    /**
     * Whether the Floodgate API is available on the server. When false, every player is
     * reported as {@link PlatformType#JAVA}.
     */
    boolean isFloodgateAvailable();
}
