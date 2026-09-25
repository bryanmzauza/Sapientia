package dev.brmz.sapientia.core.guide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.crafting.VanillaRecipe;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.guide.GuideEntry;
import dev.brmz.sapientia.api.guide.GuideService;
import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.progression.ProgressionServiceImpl;
import dev.brmz.sapientia.core.ui.UIService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link GuideService} implementation. Three-level navigation:
 * <ol>
 *   <li>Index — one button per {@link GuideCategory}.</li>
 *   <li>Category — paginated entry list (28 per page, prev/next/back).</li>
 *   <li>Detail — recipe + description, back returns to the category.</li>
 * </ol>
 * The last view is kept in the player's data, so the guide reopens where the
 * player left it. The era almanac ({@link EraAlmanac}) lists the same entries
 * by era.
 */
public final class GuideServiceImpl implements GuideService {

    static final NamespacedKey INDEX_KEY    = NamespacedKey.fromString("sapientia:guide");
    static final NamespacedKey CATEGORY_KEY = NamespacedKey.fromString("sapientia:guide_category");
    static final NamespacedKey DETAIL_KEY   = NamespacedKey.fromString("sapientia:guide_entry");

    private static final NamespacedKey ITEM_ID_PDC = NamespacedKey.fromString("sapientia:item_id");

    // Index layout (categories) — 3x3-ish inner area.
    private static final int INDEX_HEADER_SLOT = 4;
    private static final int[] INDEX_CATEGORY_SLOTS = {
            20, 22, 24,
            29, 31, 33,
    };

    // Category layout (paginated entries) — 7x4 inner area, header row + nav row.
    private static final int CATEGORY_HEADER_SLOT = 4;
    private static final int CATEGORY_PREV_SLOT   = 45;
    private static final int CATEGORY_BACK_SLOT   = 49;
    private static final int CATEGORY_NEXT_SLOT   = 53;
    private static final int[] CATEGORY_ENTRY_SLOTS = innerSlots54();
    private static final int PAGE_SIZE = 28;

    // Detail layout — unchanged.
    private static final int DETAIL_ICON_SLOT = 4;
    private static final int[] DETAIL_RECIPE_SLOTS = {
            19, 20, 21,
            28, 29, 30,
            37, 38, 39,
    };
    private static final int DETAIL_ARROW_SLOT  = 32;
    private static final int DETAIL_RESULT_SLOT = 34;
    private static final int DETAIL_BACK_SLOT   = 49;

    // Border slots reused across all three views.
    static final int[] CHEST_BORDER = chestBorderSlots54();

    private final Plugin plugin;
    final Map<NamespacedKey, GuideEntry> entries = new LinkedHashMap<>();
    private final UIService uiService;
    private final UnlockService unlockService;
    private final ProgressionServiceImpl progression;
    private final Messages messages;
    private final NamespacedKey viewKey;
    private final EraAlmanac almanac;

    public GuideServiceImpl(
            @NotNull Plugin plugin,
            @NotNull UIService uiService,
            @NotNull UnlockService unlockService,
            @NotNull ProgressionServiceImpl progression,
            @NotNull Messages messages) {
        this.plugin = plugin;
        this.uiService = uiService;
        this.unlockService = unlockService;
        this.progression = progression;
        this.messages = messages;
        uiService.register(new GuideIndexDescriptor());
        uiService.register(new GuideCategoryDescriptor());
        uiService.register(new GuideDetailDescriptor());
        this.viewKey = new NamespacedKey(plugin, "guide_view");
        this.almanac = new EraAlmanac(this, plugin, uiService, progression, messages);
    }

    @Override
    public void register(@NotNull GuideEntry entry) {
        entries.put(entry.id(), entry);
    }

    @Override
    public @NotNull Collection<GuideEntry> entries() {
        return Collections.unmodifiableCollection(new ArrayList<>(entries.values()));
    }

    @Override
    public @NotNull Collection<GuideEntry> entriesIn(@NotNull GuideCategory category) {
        return entries.values().stream().filter(e -> e.category() == category).toList();
    }

    @Override
    public @NotNull Optional<GuideEntry> find(@NotNull NamespacedKey id) {
        return Optional.ofNullable(entries.get(id));
    }

    /** Reopens the view the player left the guide on, or the index. */
    @Override
    public void open(@NotNull Player player) {
        String saved = player.getPersistentDataContainer().get(viewKey, PersistentDataType.STRING);
        String[] parts = saved == null ? new String[0] : saved.split("\\|");
        GuideCategory category = parts.length >= 3 ? categoryOf(parts[parts.length - 2]) : null;
        int page = parts.length >= 3 ? parseInt(parts[parts.length - 1]) : 0;
        if (category != null && parts[0].equals("category")) {
            openCategory(player, new CategoryView(category, page));
            return;
        }
        if (category != null && parts[0].equals("entry") && parts.length == 4) {
            NamespacedKey id = NamespacedKey.fromString(parts[1]);
            GuideEntry entry = id == null ? null : entries.get(id);
            if (entry != null && !isEraLocked(entry, player) && isVisible(entry, player)) {
                openDetail(player, entry, new CategoryView(category, page));
            } else {
                openCategory(player, new CategoryView(category, page));
            }
            return;
        }
        openIndex(player);
    }

    @Override
    public void openEras(@NotNull Player player) {
        almanac.open(player);
    }

    private void openIndex(@NotNull Player player) {
        remember(player, "index");
        uiService.open(player, new GuideIndexDescriptor(), player);
    }

    private void openCategory(@NotNull Player player, @NotNull CategoryView view) {
        remember(player, "category|" + view.category().name() + "|" + view.page());
        uiService.open(player, new GuideCategoryDescriptor(), view);
    }

    private void openDetail(@NotNull Player player, @NotNull GuideEntry entry, @NotNull CategoryView from) {
        remember(player, "entry|" + entry.id() + "|" + from.category().name() + "|" + from.page());
        openDetail(player, entry, p -> openCategory(p, from));
    }

    /** Opens an entry's detail page; its back button runs {@code back}. */
    void openDetail(@NotNull Player player, @NotNull GuideEntry entry, @NotNull Consumer<Player> back) {
        uiService.open(player, new GuideDetailDescriptor(), new DetailView(entry, back));
    }

    private void remember(@NotNull Player player, @NotNull String view) {
        player.getPersistentDataContainer().set(viewKey, PersistentDataType.STRING, view);
    }

    private static @Nullable GuideCategory categoryOf(@NotNull String name) {
        for (GuideCategory category : GuideCategory.values()) {
            if (category.name().equals(name)) return category;
        }
        return null;
    }

    static int parseInt(@NotNull String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Entries of a category, earliest era first; entries of locked eras are listed too, as locked. */
    private List<GuideEntry> entriesForCategory(@NotNull GuideCategory category, @NotNull Player player) {
        List<GuideEntry> all = new ArrayList<>();
        for (GuideEntry e : entries.values()) {
            if (e.category() == category) all.add(e);
        }
        all.sort(Comparator.comparing((GuideEntry e) -> progression.eraOf(e.id()).number())
                .thenComparing(e -> e.id().toString()));
        return all;
    }

    private int countByCategory(@NotNull GuideCategory category, @NotNull Player player) {
        return entriesForCategory(category, player).size();
    }

    /** Entries of a category whose era is not unlocked yet. */
    private int lockedInCategory(@NotNull GuideCategory category, @NotNull Player player) {
        if (progression.bypasses(player)) return 0;
        int n = 0;
        for (GuideEntry e : entries.values()) {
            if (e.category() == category && isEraLocked(e, player)) n++;
        }
        return n;
    }

    boolean isEraLocked(@NotNull GuideEntry entry, @NotNull Player player) {
        return !progression.bypasses(player) && !progression.isAvailable(entry.id());
    }

    /** "Era N: name" for an entry. */
    Component eraLine(@NotNull GuideEntry entry) {
        Era era = progression.eraOf(entry.id());
        return messages.component("era.lore",
                Placeholder.unparsed("number", Integer.toString(era.number())),
                Placeholder.component("era", messages.component(era.nameKey())));
    }

    boolean isVisible(@NotNull GuideEntry entry, @NotNull Player player) {
        return entry.discoveredByDefault() || unlockService.isUnlocked(player.getUniqueId(), entry.id());
    }

    /** Server era and the player's next goal, as lore or text lines. */
    List<Component> progressLines(@NotNull Player player) {
        List<Component> lines = new ArrayList<>();
        Era era = progression.serverEra();
        lines.add(messages.component("guide.index.era",
                Placeholder.unparsed("number", Integer.toString(era.number())),
                Placeholder.component("era", messages.component(era.nameKey()))));
        Era goal = progression.nextGoal(player.getUniqueId());
        NamespacedKey gateway = goal == null ? null : goal.gatewayItem();
        if (goal != null && gateway != null) {
            Component item = Sapientia.get().findItem(gateway)
                    .map(i -> messages.component(i.displayNameKey()))
                    .or(() -> Sapientia.get().findBlock(gateway).map(b -> messages.component(b.displayNameKey())))
                    .orElse(Component.text(gateway.getKey()));
            lines.add(messages.component("guide.index.goal",
                    Placeholder.component("item", item),
                    Placeholder.component("era", messages.component(goal.nameKey()))));
        } else {
            lines.add(messages.component("guide.index.goal-done"));
        }
        return lines;
    }

    /** Lore explaining why a recipe is locked for the player, or empty when it is not. */
    private List<Component> recipeLockLines(@NotNull Player player, @NotNull SapientiaRecipe recipe,
                                            @NotNull GuideEntry entry) {
        if (progression.bypasses(player) || progression.isRecipeUnlocked(player.getUniqueId(), recipe.id())) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        Era era = progression.eraOf(entry.id());
        if (!progression.isUnlocked(era)) {
            lines.add(messages.component("research.locked.era",
                    Placeholder.unparsed("number", Integer.toString(era.number())),
                    Placeholder.component("era", messages.component(era.nameKey()))));
            return lines;
        }
        lines.add(messages.component("research.locked.missing"));
        for (NamespacedKey missing : progression.missingFor(player.getUniqueId(), recipe.id())) {
            Component name = Sapientia.get().findItem(missing)
                    .map(i -> messages.component(i.displayNameKey()))
                    .orElse(Component.text(missing.getKey()));
            lines.add(Component.text("- ").append(name));
        }
        return lines;
    }

    /** Context for the paginated category view. */
    private record CategoryView(GuideCategory category, int page) {}

    /** Context for an entry's detail page: the entry and where its back button leads. */
    private record DetailView(GuideEntry entry, Consumer<Player> back) {}

    // -- descriptors -----------------------------------------------------------

    private final class GuideIndexDescriptor implements UIDescriptor<Player> {
        @Override public @NotNull NamespacedKey key() { return INDEX_KEY; }
        @Override public @NotNull JavaInventoryRenderer<Player> javaRenderer() { return new GuideIndexRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<Player> bedrockRenderer() {
            return new GuideIndexBedrockRenderer();
        }
    }

    private final class GuideCategoryDescriptor implements UIDescriptor<CategoryView> {
        @Override public @NotNull NamespacedKey key() { return CATEGORY_KEY; }
        @Override public @NotNull JavaInventoryRenderer<CategoryView> javaRenderer() { return new GuideCategoryRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<CategoryView> bedrockRenderer() {
            return new GuideCategoryBedrockRenderer();
        }
    }

    private final class GuideDetailDescriptor implements UIDescriptor<DetailView> {
        @Override public @NotNull NamespacedKey key() { return DETAIL_KEY; }
        @Override public @NotNull JavaInventoryRenderer<DetailView> javaRenderer() { return new GuideDetailRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<DetailView> bedrockRenderer() {
            return new GuideDetailBedrockRenderer();
        }
    }

    // -- index (categories) ----------------------------------------------------

    private final class GuideIndexRenderer implements JavaInventoryRenderer<Player> {

        private final Map<Integer, GuideCategory> slotIndex = new HashMap<>();
        private Player viewer;

        @Override public int size(@NotNull Player player, @NotNull Player ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull Player ctx) {
            return messages.component("guide.title");
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull Player ctx) {
            slotIndex.clear();
            viewer = player;
            ItemStack border = decorativePane(Material.BLACK_STAINED_GLASS_PANE);
            for (int slot : CHEST_BORDER) inventory.setItem(slot, border);
            inventory.setItem(INDEX_HEADER_SLOT, headerBook());

            GuideCategory[] categories = GuideCategory.values();
            for (int i = 0; i < categories.length && i < INDEX_CATEGORY_SLOTS.length; i++) {
                int slot = INDEX_CATEGORY_SLOTS[i];
                GuideCategory cat = categories[i];
                inventory.setItem(slot, renderCategoryButton(cat));
                slotIndex.put(slot, cat);
            }
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull Player context, int slot) {
            GuideCategory cat = slotIndex.get(slot);
            if (cat == null) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
            Bukkit.getScheduler().runTask(plugin,
                    () -> openCategory(player, new CategoryView(cat, 0)));
        }

        private ItemStack headerBook() {
            ItemStack stack = sapientiaIcon(new NamespacedKey("sapientia", "guide"), Material.WRITTEN_BOOK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.index.header.name").style(noItalic()));
                List<Component> lore = new ArrayList<>(splitLore(messages.plain("guide.index.header.lore"),
                        NamedTextColor.GRAY));
                lore.add(Component.empty());
                for (Component line : progressLines(viewer)) {
                    lore.add(line.decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderCategoryButton(GuideCategory cat) {
            ItemStack stack = categoryIcon(cat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(categoryNameKey(cat)).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                String descKey = categoryDescKey(cat);
                if (messages.hasKey(descKey)) {
                    for (String line : messages.plain(descKey).split("\\r?\\n")) {
                        lore.add(Component.text(line, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                    lore.add(Component.empty());
                }
                lore.add(messages.component("guide.index.button.count",
                                Placeholder.parsed("count", Integer.toString(countByCategory(cat, viewer))))
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    // -- category (paginated entries) -----------------------------------------

    private final class GuideCategoryRenderer implements JavaInventoryRenderer<CategoryView> {

        private final Map<Integer, GuideEntry> slotIndex = new HashMap<>();
        private boolean hasPrev;
        private boolean hasNext;
        private int shownPage;

        @Override public int size(@NotNull Player player, @NotNull CategoryView ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull CategoryView ctx) {
            return messages.component("guide.category.title",
                    Placeholder.parsed("category", messages.plain(categoryNameKey(ctx.category()))));
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull CategoryView view) {
            slotIndex.clear();
            ItemStack border = decorativePane(Material.BLACK_STAINED_GLASS_PANE);
            for (int slot : CHEST_BORDER) inventory.setItem(slot, border);

            List<GuideEntry> all = entriesForCategory(view.category(), player);
            int totalPages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            int page = Math.max(0, Math.min(view.page(), totalPages - 1));
            shownPage = page;

            inventory.setItem(CATEGORY_HEADER_SLOT, categoryHeader(view.category(), page, totalPages, all.size(),
                    lockedInCategory(view.category(), player)));

            int from = page * PAGE_SIZE;
            int to = Math.min(all.size(), from + PAGE_SIZE);
            for (int i = from, idx = 0; i < to; i++, idx++) {
                int slot = CATEGORY_ENTRY_SLOTS[idx];
                GuideEntry entry = all.get(i);
                if (isEraLocked(entry, player)) {
                    inventory.setItem(slot, renderLockedEra(entry));
                } else if (isVisible(entry, player)) {
                    inventory.setItem(slot, renderIndexEntry(entry));
                    slotIndex.put(slot, entry);
                } else {
                    inventory.setItem(slot, renderLocked(entry));
                }
            }

            hasPrev = page > 0;
            hasNext = page < totalPages - 1;
            inventory.setItem(CATEGORY_PREV_SLOT, hasPrev ? prevButton() : disabledNavButton());
            inventory.setItem(CATEGORY_NEXT_SLOT, hasNext ? nextButton() : disabledNavButton());
            inventory.setItem(CATEGORY_BACK_SLOT, indexBackButton());
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull CategoryView ctx, int slot) {
            if (slot == CATEGORY_BACK_SLOT) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
                Bukkit.getScheduler().runTask(plugin, () -> openIndex(player));
                return;
            }
            if (slot == CATEGORY_PREV_SLOT && hasPrev) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
                CategoryView next = new CategoryView(ctx.category(), shownPage - 1);
                Bukkit.getScheduler().runTask(plugin, () -> openCategory(player, next));
                return;
            }
            if (slot == CATEGORY_NEXT_SLOT && hasNext) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
                CategoryView next = new CategoryView(ctx.category(), shownPage + 1);
                Bukkit.getScheduler().runTask(plugin, () -> openCategory(player, next));
                return;
            }
            GuideEntry entry = slotIndex.get(slot);
            if (entry == null) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
            CategoryView from = new CategoryView(ctx.category(), shownPage);
            Bukkit.getScheduler().runTask(plugin, () -> openDetail(player, entry, from));
        }

        private ItemStack categoryHeader(GuideCategory cat, int page, int totalPages, int totalEntries,
                                         int lockedEntries) {
            ItemStack stack = categoryIcon(cat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(categoryNameKey(cat)).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(messages.component("guide.page.indicator",
                                Placeholder.parsed("page", Integer.toString(page + 1)),
                                Placeholder.parsed("total", Integer.toString(totalPages)))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(messages.component("guide.index.button.count",
                                Placeholder.parsed("count", Integer.toString(totalEntries)))
                        .decoration(TextDecoration.ITALIC, false));
                if (lockedEntries > 0) {
                    lore.add(messages.component("guide.category.locked-eras",
                                    Placeholder.unparsed("count", Integer.toString(lockedEntries)))
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack prevButton() {
            ItemStack stack = new ItemStack(Material.ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.page.prev.name").style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.page.prev.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack nextButton() {
            ItemStack stack = new ItemStack(Material.ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.page.next.name").style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.page.next.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack disabledNavButton() {
            return decorativePane(Material.GRAY_STAINED_GLASS_PANE);
        }

        private ItemStack indexBackButton() {
            ItemStack stack = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.category.back.name").style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.category.back.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderIndexEntry(GuideEntry entry) {
            ItemStack stack = sapientiaIcon(entry.id(), entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey()).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(messages.component("guide.detail.category",
                                Placeholder.parsed("category",
                                        messages.plain(categoryNameKey(entry.category()))))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(eraLine(entry).decoration(TextDecoration.ITALIC, false));
                String descKey = descriptionKeyFor(entry.displayNameKey());
                if (descKey != null && messages.hasKey(descKey)) {
                    lore.add(Component.empty());
                    for (String line : messages.plain(descKey).split("\\r?\\n")) {
                        lore.add(Component.text(line, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                lore.add(Component.empty());
                lore.add(messages.component("guide.entry.click")
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderLocked(GuideEntry entry) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.locked").style(noItalic()));
                meta.lore(List.of(messages.component("guide.detail.category",
                                Placeholder.parsed("category",
                                        messages.plain(categoryNameKey(entry.category()))))
                        .decoration(TextDecoration.ITALIC, false),
                        eraLine(entry).decoration(TextDecoration.ITALIC, false)));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        /** An entry of an era not unlocked yet: its name and era, no recipe. */
        private ItemStack renderLockedEra(GuideEntry entry) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.locked-era.name",
                        Placeholder.component("name", messages.component(entry.displayNameKey())))
                        .style(noItalic()));
                meta.lore(List.of(eraLine(entry).decoration(TextDecoration.ITALIC, false),
                        messages.component("guide.locked-era.lore").decoration(TextDecoration.ITALIC, false)));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    // -- detail ---------------------------------------------------------------

    private final class GuideDetailRenderer implements JavaInventoryRenderer<DetailView> {

        @Override public int size(@NotNull Player player, @NotNull DetailView ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull DetailView ctx) {
            return messages.component("guide.detail.title",
                    Placeholder.parsed("name", messages.plain(ctx.entry().displayNameKey())));
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull DetailView view) {
            GuideEntry entry = view.entry();
            ItemStack border = decorativePane(Material.BLUE_STAINED_GLASS_PANE);
            for (int slot : CHEST_BORDER) inventory.setItem(slot, border);

            inventory.setItem(DETAIL_ICON_SLOT, headerIcon(entry));

            SapientiaRecipe recipe = findRecipeFor(entry.id());
            if (recipe != null) {
                List<RecipeIngredient> pattern = recipe.pattern();
                for (int i = 0; i < DETAIL_RECIPE_SLOTS.length && i < pattern.size(); i++) {
                    inventory.setItem(DETAIL_RECIPE_SLOTS[i], renderIngredient(pattern.get(i)));
                }
                List<Component> lock = recipeLockLines(player, recipe, entry);
                inventory.setItem(DETAIL_ARROW_SLOT, lock.isEmpty() ? arrow() : lockedArrow(lock));
                inventory.setItem(DETAIL_RESULT_SLOT, renderResult(recipe));
            } else if (findVanillaFor(entry.id()) != null) {
                VanillaRecipe vanilla = findVanillaFor(entry.id());
                List<RecipeIngredient> grid = vanillaGrid(vanilla);
                for (int i = 0; i < DETAIL_RECIPE_SLOTS.length && i < grid.size(); i++) {
                    inventory.setItem(DETAIL_RECIPE_SLOTS[i], renderIngredient(grid.get(i)));
                }
                inventory.setItem(DETAIL_ARROW_SLOT, vanillaArrow());
                ItemStack result = Sapientia.get().createStack(vanilla.result(), vanilla.amount())
                        .orElseGet(() -> new ItemStack(entry.icon()));
                ItemMeta meta = result.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                    lore.add(Component.empty());
                    lore.add(messages.component("guide.detail.yields",
                                    Placeholder.parsed("amount", Integer.toString(vanilla.amount())))
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    result.setItemMeta(meta);
                }
                inventory.setItem(DETAIL_RESULT_SLOT, result);
            } else {
                ItemStack noRecipe = new ItemStack(Material.BARRIER);
                ItemMeta meta = noRecipe.getItemMeta();
                if (meta != null) {
                    meta.displayName(messages.component("guide.detail.recipe.header").style(noItalic()));
                    meta.lore(splitLore(messages.plain("guide.detail.recipe.none"), NamedTextColor.GRAY));
                    noRecipe.setItemMeta(meta);
                }
                inventory.setItem(DETAIL_RECIPE_SLOTS[4], noRecipe);
            }

            inventory.setItem(DETAIL_BACK_SLOT, backButton());
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull DetailView context, int slot) {
            if (slot != DETAIL_BACK_SLOT) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            Bukkit.getScheduler().runTask(plugin, () -> context.back().accept(player));
        }

        private ItemStack headerIcon(GuideEntry entry) {
            ItemStack stack = sapientiaIcon(entry.id(), entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey()).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(messages.component("guide.detail.category",
                                Placeholder.parsed("category",
                                        messages.plain(categoryNameKey(entry.category()))))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(eraLine(entry).decoration(TextDecoration.ITALIC, false));
                String descKey = descriptionKeyFor(entry.displayNameKey());
                if (descKey != null && messages.hasKey(descKey)) {
                    lore.add(Component.empty());
                    for (String line : messages.plain(descKey).split("\\r?\\n")) {
                        lore.add(Component.text(line, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderIngredient(RecipeIngredient ingredient) {
            return switch (ingredient) {
                case RecipeIngredient.Empty ignored -> decorativePane(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                case RecipeIngredient.Vanilla v -> new ItemStack(v.material(), Math.max(1, v.amount()));
                case RecipeIngredient.Sapientia sap -> {
                    ItemStack s = Sapientia.get().createStack(sap.id(), Math.max(1, sap.amount())).orElse(null);
                    if (s == null) s = new ItemStack(Material.BARRIER, Math.max(1, sap.amount()));
                    yield s;
                }
            };
        }

        private ItemStack renderResult(SapientiaRecipe recipe) {
            ItemStack base = recipe.result().clone();
            ItemMeta meta = base.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null
                        ? new ArrayList<>(meta.lore())
                        : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(messages.component("guide.detail.yields",
                                Placeholder.parsed("amount", Integer.toString(base.getAmount())))
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                base.setItemMeta(meta);
            }
            return base;
        }

        private ItemStack arrow() {
            ItemStack stack = new ItemStack(Material.ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.detail.recipe.header").style(noItalic()));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack vanillaArrow() {
            ItemStack stack = new ItemStack(Material.CRAFTING_TABLE);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.detail.vanilla.name").style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.detail.vanilla.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack lockedArrow(List<Component> lines) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("research.locked.name").style(noItalic()));
                meta.lore(lines.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack backButton() {
            ItemStack stack = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.detail.back.name").style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.detail.back.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    // -- bedrock --------------------------------------------------------------

    private final class GuideIndexBedrockRenderer
            implements dev.brmz.sapientia.api.ui.BedrockFormRenderer<Player> {

        @Override
        public void open(@NotNull Player player, @NotNull Player ctx) {
            String title = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.title"));
            StringBuilder content = new StringBuilder(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.index.header.lore"))).append("\n");
            for (Component line : progressLines(player)) {
                content.append('\n').append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(line));
            }
            dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm form =
                    new dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm()
                            .title(title)
                            .content(content.toString());
            GuideCategory[] cats = GuideCategory.values();
            for (GuideCategory c : cats) {
                form.button(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component(categoryNameKey(c))));
            }
            form.onClick(idx -> {
                if (idx < 0 || idx >= cats.length) return;
                GuideCategory chosen = cats[idx];
                Bukkit.getScheduler().runTask(plugin,
                        () -> openCategory(player, new CategoryView(chosen, 0)));
            });
            form.send(player);
        }
    }

    private final class GuideCategoryBedrockRenderer
            implements dev.brmz.sapientia.api.ui.BedrockFormRenderer<CategoryView> {

        @Override
        public void open(@NotNull Player player, @NotNull CategoryView view) {
            List<GuideEntry> all = entriesForCategory(view.category(), player);
            // Entries of locked eras are listed with their era but open nothing.
            List<GuideEntry> visible = new ArrayList<>();
            for (GuideEntry e : all) {
                if (isEraLocked(e, player) || isVisible(e, player)) {
                    visible.add(e);
                }
            }

            String title = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.category.title",
                            Placeholder.parsed("category",
                                    messages.plain(categoryNameKey(view.category())))));
            String content = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.index.button.count",
                            Placeholder.parsed("count", Integer.toString(visible.size()))));

            dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm form =
                    new dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm()
                            .title(title)
                            .content(content);
            for (GuideEntry e : visible) {
                Component label = isEraLocked(e, player)
                        ? messages.component("guide.locked-era.bedrock",
                                Placeholder.component("name", messages.component(e.displayNameKey())),
                                Placeholder.unparsed("number", Integer.toString(progression.eraOf(e.id()).number())))
                        : messages.component(e.displayNameKey());
                form.button(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(label));
            }
            // Append an explicit back-to-index button.
            String backLabel = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.category.back.name"));
            form.button(backLabel);

            final List<GuideEntry> snapshot = visible;
            form.onClick(idx -> {
                if (idx < 0) return;
                if (idx == snapshot.size()) {
                    Bukkit.getScheduler().runTask(plugin, () -> openIndex(player));
                    return;
                }
                if (idx >= snapshot.size()) return;
                GuideEntry chosen = snapshot.get(idx);
                if (isEraLocked(chosen, player)) {
                    Bukkit.getScheduler().runTask(plugin, () -> openCategory(player, view));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> openDetail(player, chosen, view));
            });
            form.send(player);
        }
    }

    private final class GuideDetailBedrockRenderer
            implements dev.brmz.sapientia.api.ui.BedrockFormRenderer<DetailView> {

        @Override
        public void open(@NotNull Player player, @NotNull DetailView view) {
            GuideEntry entry = view.entry();
            String title = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.title",
                            Placeholder.parsed("name", messages.plain(entry.displayNameKey()))));

            StringBuilder body = new StringBuilder();
            body.append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.category",
                            Placeholder.parsed("category",
                                    messages.plain(categoryNameKey(entry.category()))))))
                    .append('\n')
                    .append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(eraLine(entry)))
                    .append('\n');
            String descKey = descriptionKeyFor(entry.displayNameKey());
            if (descKey != null && messages.hasKey(descKey)) {
                body.append('\n').append(messages.plain(descKey)).append('\n');
            }
            SapientiaRecipe recipe = findRecipeFor(entry.id());
            body.append('\n');
            if (recipe != null) {
                body.append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component("guide.detail.recipe.header"))).append('\n');
                List<RecipeIngredient> pattern = recipe.pattern();
                for (int i = 0; i < pattern.size(); i++) {
                    body.append("• ").append(describeIngredient(pattern.get(i))).append('\n');
                }
                body.append('\n').append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component("guide.detail.yields",
                                Placeholder.parsed("amount",
                                        Integer.toString(recipe.result().getAmount())))));
                List<Component> lock = recipeLockLines(player, recipe, entry);
                if (!lock.isEmpty()) {
                    body.append("\n\n").append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                            messages.component("research.locked.name")));
                    for (Component line : lock) {
                        body.append('\n').append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(line));
                    }
                }
            } else if (findVanillaFor(entry.id()) != null) {
                VanillaRecipe vanilla = findVanillaFor(entry.id());
                body.append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component("guide.detail.vanilla.name"))).append('\n');
                for (RecipeIngredient cell : vanillaGrid(vanilla)) {
                    body.append("• ").append(describeIngredient(cell)).append('\n');
                }
                body.append('\n').append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component("guide.detail.yields",
                                Placeholder.parsed("amount", Integer.toString(vanilla.amount())))));
            } else {
                body.append(messages.plain("guide.detail.recipe.none"));
            }

            String backLabel = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.back.name"));
            new dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm()
                    .title(title)
                    .content(body.toString())
                    .button(backLabel)
                    .onClick(idx -> Bukkit.getScheduler().runTask(plugin, () -> view.back().accept(player)))
                    .send(player);
        }

        private String describeIngredient(RecipeIngredient ingredient) {
            return switch (ingredient) {
                case RecipeIngredient.Empty ignored -> "—";
                case RecipeIngredient.Vanilla v -> v.material().name() + " x" + Math.max(1, v.amount());
                case RecipeIngredient.Sapientia sap -> sap.id().toString() + " x" + Math.max(1, sap.amount());
            };
        }
    }

    // -- helpers ---------------------------------------------------------------

    /** The vanilla crafting table recipe that makes {@code itemId}, if any. */
    private static @org.jetbrains.annotations.Nullable VanillaRecipe findVanillaFor(NamespacedKey itemId) {
        for (VanillaRecipe recipe : Sapientia.get().recipes().vanillaRecipes()) {
            if (recipe.result().equals(itemId)) return recipe;
        }
        return null;
    }

    /** A vanilla recipe laid out on a 3×3 grid, one representative material per cell. */
    private static List<RecipeIngredient> vanillaGrid(VanillaRecipe recipe) {
        List<RecipeIngredient> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) cells.add(RecipeIngredient.empty());
        if (recipe.isShaped()) {
            for (int row = 0; row < recipe.shape().size(); row++) {
                String line = recipe.shape().get(row);
                for (int col = 0; col < line.length(); col++) {
                    java.util.Set<Material> choice = recipe.ingredients().get(line.charAt(col));
                    if (choice != null) cells.set(row * 3 + col, RecipeIngredient.of(representative(choice)));
                }
            }
        } else {
            int i = 0;
            for (java.util.Set<Material> choice : recipe.ingredients().values()) {
                if (i < 9) cells.set(i++, RecipeIngredient.of(representative(choice)));
            }
        }
        return cells;
    }

    /** The material shown for a choice: the familiar one when present (oak planks, cobblestone). */
    private static Material representative(java.util.Set<Material> choice) {
        for (Material preferred : List.of(Material.OAK_PLANKS, Material.COBBLESTONE)) {
            if (choice.contains(preferred)) return preferred;
        }
        return choice.stream().min(Comparator.comparing(Material::name)).orElseThrow();
    }

    private SapientiaRecipe findRecipeFor(NamespacedKey itemId) {
        String target = itemId.toString();
        for (SapientiaRecipe r : Sapientia.get().recipes().all()) {
            ItemStack result = r.result();
            if (result == null) continue;
            ItemMeta meta = result.getItemMeta();
            if (meta == null) continue;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (ITEM_ID_PDC == null) continue;
            String tagged = pdc.get(ITEM_ID_PDC, PersistentDataType.STRING);
            if (target.equals(tagged)) return r;
        }
        return null;
    }

    static String descriptionKeyFor(String displayNameKey) {
        if (displayNameKey.endsWith(".name")) {
            return displayNameKey.substring(0, displayNameKey.length() - ".name".length()) + ".desc";
        }
        return null;
    }

    static String categoryNameKey(GuideCategory cat) {
        return "guide.category." + cat.name().toLowerCase(Locale.ROOT) + ".name";
    }

    private static String categoryDescKey(GuideCategory cat) {
        return "guide.category." + cat.name().toLowerCase(Locale.ROOT) + ".lore";
    }

    /** Representative built-in item per category, with a vanilla fallback when it is not registered. */
    private record CategoryIcon(NamespacedKey item, Material fallback) {}

    private static final Map<GuideCategory, CategoryIcon> CATEGORY_ICONS = new EnumMap<>(GuideCategory.class);
    static {
        CATEGORY_ICONS.put(GuideCategory.MATERIAL,  categoryIcon("copper_ingot", Material.IRON_INGOT));
        CATEGORY_ICONS.put(GuideCategory.TOOL,      categoryIcon("wrench", Material.IRON_PICKAXE));
        CATEGORY_ICONS.put(GuideCategory.MACHINE,   categoryIcon("macerator", Material.FURNACE));
        CATEGORY_ICONS.put(GuideCategory.ENERGY,    categoryIcon("generator", Material.REDSTONE));
        CATEGORY_ICONS.put(GuideCategory.LOGISTICS, categoryIcon("item_cable", Material.HOPPER));
        CATEGORY_ICONS.put(GuideCategory.MISC,      categoryIcon("guide", Material.BOOK));
    }

    private static CategoryIcon categoryIcon(String item, Material fallback) {
        return new CategoryIcon(new NamespacedKey("sapientia", item), fallback);
    }

    private static ItemStack categoryIcon(GuideCategory cat) {
        CategoryIcon icon = CATEGORY_ICONS.get(cat);
        return icon == null ? new ItemStack(Material.BOOK) : sapientiaIcon(icon.item(), icon.fallback());
    }

    /**
     * Display stack for a Sapientia item, built through the item registry so it
     * carries the item's bundled model. Falls back to the plain base material
     * for ids that are not registered items (callers overwrite name and lore).
     */
    static ItemStack sapientiaIcon(NamespacedKey id, Material fallback) {
        return Sapientia.get().createStack(id, 1).orElseGet(() -> new ItemStack(fallback));
    }

    static List<Component> splitLore(String raw, NamedTextColor color) {
        List<Component> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            out.add(Component.text(line, color).decoration(TextDecoration.ITALIC, false));
        }
        return out;
    }

    static ItemStack decorativePane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static Style noItalic() {
        return Style.style().decoration(TextDecoration.ITALIC, false).build();
    }

    /**
     * Outer-perimeter slots of a 54-slot chest (rows 0 and 5, plus left/right edges),
     * minus the slots reserved for header (4), prev (45), back (49) and next (53).
     */
    private static int[] chestBorderSlots54() {
        List<Integer> out = new ArrayList<>(28);
        for (int i = 0; i < 9; i++) out.add(i);
        for (int row = 1; row <= 4; row++) {
            out.add(row * 9);
            out.add(row * 9 + 8);
        }
        for (int i = 45; i < 54; i++) out.add(i);
        out.removeIf(s -> s == INDEX_HEADER_SLOT
                       || s == CATEGORY_PREV_SLOT
                       || s == CATEGORY_BACK_SLOT
                       || s == CATEGORY_NEXT_SLOT);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    /** Inner 7x4 area = rows 1..4, columns 1..7 (28 slots). */
    static int[] innerSlots54() {
        int[] arr = new int[28];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                arr[idx++] = row * 9 + col;
            }
        }
        return arr;
    }
}
