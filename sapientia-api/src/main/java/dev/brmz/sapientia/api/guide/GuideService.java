package dev.brmz.sapientia.api.guide;

import java.util.Collection;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * In-game guide service (T-150 / 0.4.0). Sapientia auto-populates one entry per
 * registered item or block; addons can add custom entries too. Opening the
 * guide routes through the {@code UIService} to render it on Java or Bedrock.
 */
public interface GuideService {

    /** Registers an additional guide entry (auto-entries cover registered items/blocks). */
    void register(@NotNull GuideEntry entry);

    /** Snapshot of every entry currently known to the guide, in insertion order. */
    @NotNull Collection<GuideEntry> entries();

    /** Subset of {@link #entries()} for a single category, in insertion order. */
    @NotNull Collection<GuideEntry> entriesIn(@NotNull GuideCategory category);

    /** Looks up a specific entry by its id. */
    @NotNull java.util.Optional<GuideEntry> find(@NotNull NamespacedKey id);

    /** Opens the guide UI for the given player. */
    void open(@NotNull Player player);
}
