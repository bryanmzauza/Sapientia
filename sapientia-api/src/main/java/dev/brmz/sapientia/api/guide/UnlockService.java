package dev.brmz.sapientia.api.guide;

import java.util.Set;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Per-player unlock tracker (T-151 / 0.4.0). Starts empty for each player;
 * entries are unlocked by gameplay (crafting, machine completion, quest hooks).
 * Entries with {@link GuideEntry#discoveredByDefault()} are always treated as
 * unlocked regardless of what this service returns.
 */
public interface UnlockService {

    /** Marks {@code entryId} as unlocked for the given player. Returns true if this was a new unlock. */
    boolean unlock(@NotNull UUID player, @NotNull NamespacedKey entryId);

    /** Whether the player has already unlocked the entry. */
    boolean isUnlocked(@NotNull UUID player, @NotNull NamespacedKey entryId);

    /** Snapshot of all entry ids currently unlocked for the player. */
    @NotNull Set<NamespacedKey> unlockedFor(@NotNull UUID player);
}
