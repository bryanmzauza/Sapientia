package dev.brmz.sapientia.core.progression;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dev.brmz.sapientia.api.events.SapientiaDiscoveryEvent;
import dev.brmz.sapientia.api.events.SapientiaEraChangeEvent;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.api.progression.ProgressionService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link ProgressionService}. The server era and each online player's
 * discoveries live in memory; loads and writes go through the database thread,
 * so nothing here touches the database on the main thread.
 */
public final class ProgressionServiceImpl implements ProgressionService {

    /** Permission that ignores era locks. */
    public static final String BYPASS_PERMISSION = "sapientia.era.bypass";

    private final ProgressionStore store;
    private final Consumer<Runnable> database;
    private final Executor mainThread;
    private final Function<NamespacedKey, Era> eraResolver;
    private final Predicate<NamespacedKey> registered;
    private final Consumer<Event> events;
    private final Predicate<UUID> online;
    private final boolean research;

    private final Map<NamespacedKey, Era> eraCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<NamespacedKey>> discoveries = new ConcurrentHashMap<>();
    private Supplier<Collection<ResearchBook.Entry>> recipeSource = List::of;
    private Supplier<Integer> recipeRevision = () -> 0;
    private ResearchBook book = new ResearchBook(List.of());
    private int bookRevision = -1;
    private volatile Era serverEra;

    /**
     * @param eraResolver era of an item, block or recipe id ({@code null} when unknown)
     * @param registered  whether an item id is registered (to skip gateway items that do not exist yet)
     * @param online      whether a player is online (loads for players who left are dropped)
     */
    public ProgressionServiceImpl(@NotNull ProgressionStore store, @NotNull Consumer<Runnable> database,
                                  @NotNull Executor mainThread, @NotNull Era serverEra, boolean research,
                                  @NotNull Function<NamespacedKey, Era> eraResolver,
                                  @NotNull Predicate<NamespacedKey> registered,
                                  @NotNull Consumer<Event> events, @NotNull Predicate<UUID> online) {
        this.store = store;
        this.database = database;
        this.mainThread = mainThread;
        this.serverEra = serverEra;
        this.research = research;
        this.eraResolver = eraResolver;
        this.registered = registered;
        this.events = events;
        this.online = online;
    }

    /** Where workbench recipes come from; the index is rebuilt when the revision changes. */
    public void setRecipes(@NotNull Supplier<Collection<ResearchBook.Entry>> source, @NotNull Supplier<Integer> revision) {
        this.recipeSource = source;
        this.recipeRevision = revision;
        this.bookRevision = -1;
    }

    // --- Eras ------------------------------------------------------------------------

    @Override
    public @NotNull Era serverEra() {
        return serverEra;
    }

    @Override
    public void setServerEra(@NotNull Era era) {
        Era previous = serverEra;
        if (previous == era) return;
        serverEra = era;
        database.accept(() -> store.saveEra(era));
        events.accept(new SapientiaEraChangeEvent(previous, era));
    }

    @Override
    public boolean isUnlocked(@NotNull Era era) {
        return !era.isAfter(serverEra);
    }

    @Override
    public @NotNull Era eraOf(@NotNull NamespacedKey contentId) {
        Era cached = eraCache.get(contentId);
        if (cached != null) return cached;
        Era resolved = eraResolver.apply(contentId);
        if (resolved == null) return Era.ARRIVAL; // unknown ids are not cached: they may register later
        eraCache.put(contentId, resolved);
        return resolved;
    }

    @Override
    public boolean bypasses(@NotNull Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }

    // --- Research ----------------------------------------------------------------------

    @Override
    public boolean researchEnabled() {
        return research;
    }

    @Override
    public boolean hasDiscovered(@NotNull UUID player, @NotNull NamespacedKey itemId) {
        Set<NamespacedKey> set = discoveries.get(player);
        return set != null && set.contains(itemId);
    }

    @Override
    public boolean discover(@NotNull UUID player, @NotNull NamespacedKey itemId) {
        Set<NamespacedKey> set = discoveries.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
        if (!set.add(itemId)) return false;
        store.queueDiscovery(player, itemId);
        List<NamespacedKey> unlocked = research
                ? book().unlockedBy(itemId, set, this::recipeEraUnlocked)
                : List.of();
        events.accept(new SapientiaDiscoveryEvent(player, itemId, unlocked));
        return true;
    }

    @Override
    public @NotNull Set<NamespacedKey> discoveries(@NotNull UUID player) {
        Set<NamespacedKey> set = discoveries.get(player);
        return set == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(set));
    }

    @Override
    public boolean isRecipeUnlocked(@NotNull UUID player, @NotNull NamespacedKey recipeId) {
        ResearchBook.Entry entry = book().entry(recipeId);
        if (entry == null) return isUnlocked(eraOf(recipeId));
        if (!recipeEraUnlocked(entry)) return false;
        if (!research) return true;
        Set<NamespacedKey> set = discoveries.getOrDefault(player, Set.of());
        return set.containsAll(entry.prerequisites());
    }

    /** Ingredients of a recipe the player still has to discover. */
    public @NotNull List<NamespacedKey> missingFor(@NotNull UUID player, @NotNull NamespacedKey recipeId) {
        if (!research) return List.of();
        return book().missing(recipeId, discoveries.getOrDefault(player, Set.of()));
    }

    @Override
    public @Nullable Era nextGoal(@NotNull UUID player) {
        for (int n = 0; n <= serverEra.number(); n++) {
            Era era = Era.of(n);
            NamespacedKey gateway = era.gatewayItem();
            if (gateway == null || !registered.test(gateway)) continue;
            if (!hasDiscovered(player, gateway)) return era;
        }
        return null;
    }

    // --- Player data ---------------------------------------------------------------------

    /**
     * Loads a player's discoveries on the database thread and merges them on the
     * main thread; {@code then} runs after the merge.
     */
    public void load(@NotNull UUID player, @NotNull Runnable then) {
        database.accept(() -> {
            Set<NamespacedKey> loaded = store.loadDiscoveries(player);
            mainThread.execute(() -> {
                if (!online.test(player)) return;
                discoveries.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).addAll(loaded);
                then.run();
            });
        });
    }

    /** Drops a player's discoveries from memory (they stay in the database). */
    public void evict(@NotNull UUID player) {
        discoveries.remove(player);
    }

    private boolean recipeEraUnlocked(ResearchBook.Entry entry) {
        NamespacedKey id = entry.result() != null ? entry.result() : entry.recipe();
        return isUnlocked(eraOf(id));
    }

    private ResearchBook book() {
        int revision = recipeRevision.get();
        if (revision != bookRevision) {
            book = new ResearchBook(recipeSource.get());
            bookRevision = revision;
        }
        return book;
    }

    /** The research index (rebuilt when recipes change). */
    public @NotNull ResearchBook researchBook() {
        return book();
    }
}
