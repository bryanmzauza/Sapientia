package dev.brmz.sapientia.core.progression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What each workbench recipe needs to be researched: the Sapientia items among
 * its ingredients. Indexed both ways so a discovery finds the recipes it may
 * unlock without scanning every recipe.
 */
public final class ResearchBook {

    /** A recipe as research sees it. */
    public record Entry(@NotNull NamespacedKey recipe, @Nullable NamespacedKey result,
                        @NotNull Set<NamespacedKey> prerequisites) {
        public Entry {
            prerequisites = Set.copyOf(prerequisites);
        }
    }

    private final Map<NamespacedKey, Entry> byRecipe = new HashMap<>();
    private final Map<NamespacedKey, List<Entry>> byPrerequisite = new HashMap<>();
    private final Map<NamespacedKey, List<Entry>> byResult = new HashMap<>();

    public ResearchBook(@NotNull Collection<Entry> entries) {
        for (Entry entry : entries) {
            byRecipe.put(entry.recipe(), entry);
            for (NamespacedKey prerequisite : entry.prerequisites()) {
                byPrerequisite.computeIfAbsent(prerequisite, k -> new ArrayList<>()).add(entry);
            }
            if (entry.result() != null) {
                byResult.computeIfAbsent(entry.result(), k -> new ArrayList<>()).add(entry);
            }
        }
    }

    public @Nullable Entry entry(@NotNull NamespacedKey recipe) {
        return byRecipe.get(recipe);
    }

    /** Recipes that produce {@code item}. */
    public @NotNull List<Entry> recipesFor(@NotNull NamespacedKey item) {
        return byResult.getOrDefault(item, List.of());
    }

    /** Prerequisites of {@code recipe} missing from {@code discoveries}. */
    public @NotNull List<NamespacedKey> missing(@NotNull NamespacedKey recipe, @NotNull Set<NamespacedKey> discoveries) {
        Entry entry = byRecipe.get(recipe);
        if (entry == null) return List.of();
        List<NamespacedKey> out = new ArrayList<>();
        for (NamespacedKey prerequisite : entry.prerequisites()) {
            if (!discoveries.contains(prerequisite)) out.add(prerequisite);
        }
        return out;
    }

    /**
     * Recipes that became complete because {@code discovered} was just added to
     * {@code discoveries} (which already contains it), limited to recipes that
     * {@code available} accepts (their era is unlocked).
     */
    public @NotNull List<NamespacedKey> unlockedBy(@NotNull NamespacedKey discovered,
                                                   @NotNull Set<NamespacedKey> discoveries,
                                                   @NotNull Predicate<Entry> available) {
        List<NamespacedKey> out = new ArrayList<>();
        for (Entry entry : byPrerequisite.getOrDefault(discovered, List.of())) {
            if (available.test(entry) && discoveries.containsAll(entry.prerequisites())) {
                out.add(entry.recipe());
            }
        }
        return out;
    }
}
