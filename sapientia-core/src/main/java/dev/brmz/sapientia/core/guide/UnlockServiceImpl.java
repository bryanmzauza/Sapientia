package dev.brmz.sapientia.core.guide;

import java.util.Set;
import java.util.UUID;

import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.progression.ProgressionService;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * {@link UnlockService} on top of player research: an entry is unlocked when
 * the player has discovered it or can craft it. Kept for addons written
 * against the earlier API; new code should use {@link ProgressionService}.
 */
public final class UnlockServiceImpl implements UnlockService {

    private final ProgressionService progression;

    public UnlockServiceImpl(@NotNull ProgressionService progression) {
        this.progression = progression;
    }

    @Override
    public boolean unlock(@NotNull UUID player, @NotNull NamespacedKey entryId) {
        return progression.discover(player, entryId);
    }

    @Override
    public boolean isUnlocked(@NotNull UUID player, @NotNull NamespacedKey entryId) {
        return progression.hasDiscovered(player, entryId) || progression.isRecipeUnlocked(player, entryId);
    }

    @Override
    public @NotNull Set<NamespacedKey> unlockedFor(@NotNull UUID player) {
        return progression.discoveries(player);
    }
}
