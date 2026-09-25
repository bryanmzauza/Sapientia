package dev.brmz.sapientia.api.progression;

import java.util.Set;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server era and per-player research.
 *
 * <p><b>Eras.</b> The server has a current era set by the admin; every era up
 * to it is unlocked. Each item and block belongs to an era
 * ({@link dev.brmz.sapientia.api.item.SapientiaItem#era()},
 * {@link dev.brmz.sapientia.api.block.SapientiaBlock#era()}); content of a
 * locked era cannot be crafted, placed, processed or found in the world.
 *
 * <p><b>Research.</b> A player discovers an item by crafting it, picking it up,
 * taking it out of a container or harvesting it. A recipe is unlocked for a
 * player when its era is unlocked and every Sapientia ingredient has been
 * discovered. With research turned off in the configuration, every recipe of
 * an unlocked era is unlocked for everyone.
 *
 * <p>Player data is loaded in the background when the player joins; until it
 * arrives (or for offline players) discovery queries answer {@code false}.
 */
public interface ProgressionService {

    /** The server's current era: the highest unlocked era. */
    @NotNull Era serverEra();

    /**
     * Sets the server era (any era, higher or lower). Fires
     * {@link dev.brmz.sapientia.api.events.SapientiaEraChangeEvent}.
     */
    void setServerEra(@NotNull Era era);

    /** Whether {@code era} is unlocked on this server. */
    boolean isUnlocked(@NotNull Era era);

    /** Era of a registered item, block or recipe result; era 0 for unknown ids. */
    @NotNull Era eraOf(@NotNull NamespacedKey contentId);

    /** Whether the content's era is unlocked. */
    default boolean isAvailable(@NotNull NamespacedKey contentId) {
        return isUnlocked(eraOf(contentId));
    }

    /** Whether the player may ignore era locks ({@code sapientia.era.bypass}). */
    boolean bypasses(@NotNull Player player);

    /** Whether the player has discovered the item. */
    boolean hasDiscovered(@NotNull UUID player, @NotNull NamespacedKey itemId);

    /**
     * Records a discovery. Returns {@code true} when it is new. Fires
     * {@link dev.brmz.sapientia.api.events.SapientiaDiscoveryEvent}.
     */
    boolean discover(@NotNull UUID player, @NotNull NamespacedKey itemId);

    /** Every item the player has discovered. */
    @NotNull Set<NamespacedKey> discoveries(@NotNull UUID player);

    /** Whether the workbench recipe is unlocked for the player (era and research). */
    boolean isRecipeUnlocked(@NotNull UUID player, @NotNull NamespacedKey recipeId);

    /** Whether research is on (recipes need their ingredients discovered). */
    boolean researchEnabled();

    /**
     * The player's next goal: the gateway item of the lowest unlocked era the
     * player has not completed, or {@code null} when every unlocked era is done.
     */
    @Nullable Era nextGoal(@NotNull UUID player);
}
