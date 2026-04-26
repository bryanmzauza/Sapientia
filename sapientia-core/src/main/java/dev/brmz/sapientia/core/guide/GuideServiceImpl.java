package dev.brmz.sapientia.core.guide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.SapientiaRecipe;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.guide.GuideEntry;
import dev.brmz.sapientia.api.guide.GuideService;
import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.core.i18n.Messages;
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

/**
 * Default {@link GuideService} implementation (T-150 / 0.4.0). Auto-populates
 * entries from the content registries at start-up and renders them as a 54-slot
 * chest. Clicking an unlocked entry opens a detail UI showing the recipe, the
 * full description and category metadata. Locked entries render as grey panes.
 */
public final class GuideServiceImpl implements GuideService {

    static final NamespacedKey INDEX_KEY = NamespacedKey.fromString("sapientia:guide");
    static final NamespacedKey DETAIL_KEY = NamespacedKey.fromString("sapientia:guide_entry");
    private static final NamespacedKey ITEM_ID_PDC = NamespacedKey.fromString("sapientia:item_id");

    // Slot constants (declared before the arrays so the array builders can read them).
    private static final int INDEX_HEADER_SLOT = 4;
    private static final int DETAIL_ICON_SLOT = 4;
    private static final int[] DETAIL_RECIPE_SLOTS = {
            19, 20, 21,
            28, 29, 30,
            37, 38, 39,
    };
    private static final int DETAIL_ARROW_SLOT = 32;
    private static final int DETAIL_RESULT_SLOT = 34;
    private static final int DETAIL_BACK_SLOT = 49;

    // Index layout: glass-pane border with the inner 7x4 area (28 slots) reserved
    // for entry icons. Slot 4 holds the "you are here" header book.
    private static final int[] INDEX_BORDER = borderSlots54();
    private static final int[] INDEX_ENTRY_SLOTS = innerSlots54();

    private final Plugin plugin;
    private final Map<NamespacedKey, GuideEntry> entries = new LinkedHashMap<>();
    private final UIService uiService;
    private final UnlockService unlockService;
    private final Messages messages;

    public GuideServiceImpl(
            @NotNull Plugin plugin,
            @NotNull UIService uiService,
            @NotNull UnlockService unlockService,
            @NotNull Messages messages) {
        this.plugin = plugin;
        this.uiService = uiService;
        this.unlockService = unlockService;
        this.messages = messages;
        uiService.register(new GuideIndexDescriptor());
        uiService.register(new GuideDetailDescriptor());
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

    @Override
    public void open(@NotNull Player player) {
        uiService.open(player, new GuideIndexDescriptor(), player);
    }

    private void openDetail(@NotNull Player player, @NotNull GuideEntry entry) {
        uiService.open(player, new GuideDetailDescriptor(), entry);
    }

    // --------------------------------------------------------------- descriptors

    private final class GuideIndexDescriptor implements UIDescriptor<Player> {
        @Override public @NotNull NamespacedKey key() { return INDEX_KEY; }
        @Override public @NotNull JavaInventoryRenderer<Player> javaRenderer() { return new GuideIndexRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<Player> bedrockRenderer() {
            return new GuideIndexBedrockRenderer();
        }
    }

    private final class GuideDetailDescriptor implements UIDescriptor<GuideEntry> {
        @Override public @NotNull NamespacedKey key() { return DETAIL_KEY; }
        @Override public @NotNull JavaInventoryRenderer<GuideEntry> javaRenderer() { return new GuideDetailRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<GuideEntry> bedrockRenderer() {
            return new GuideDetailBedrockRenderer();
        }
    }

    // --------------------------------------------------------------- index

    private final class GuideIndexRenderer implements JavaInventoryRenderer<Player> {

        // slot -> entry mapping rebuilt every render so the click handler can map
        // a click back to the entry that occupied that slot.
        private final Map<Integer, GuideEntry> slotIndex = new HashMap<>();

        @Override public int size(@NotNull Player player, @NotNull Player ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull Player ctx) {
            return messages.component("guide.title");
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull Player ctx) {
            slotIndex.clear();
            // Decorative border.
            ItemStack border = decorativePane(Material.BLACK_STAINED_GLASS_PANE);
            for (int slot : INDEX_BORDER) {
                inventory.setItem(slot, border);
            }
            // Header book at the top centre.
            inventory.setItem(INDEX_HEADER_SLOT, headerBook());

            // Sort entries by category then id for stable layout.
            List<GuideEntry> sorted = new ArrayList<>(entries.values());
            sorted.sort(Comparator.comparing((GuideEntry e) -> e.category().ordinal())
                    .thenComparing(e -> e.id().toString()));

            int idx = 0;
            for (GuideEntry entry : sorted) {
                if (idx >= INDEX_ENTRY_SLOTS.length) break;
                int slot = INDEX_ENTRY_SLOTS[idx++];
                boolean unlocked = entry.discoveredByDefault()
                        || unlockService.isUnlocked(player.getUniqueId(), entry.id());
                if (unlocked) {
                    inventory.setItem(slot, renderIndexEntry(entry));
                    slotIndex.put(slot, entry);
                } else {
                    inventory.setItem(slot, renderLocked(entry));
                }
            }
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull Player context, int slot) {
            GuideEntry entry = slotIndex.get(slot);
            if (entry == null) return;
            // Defer: opening another inventory inside an InventoryClickEvent
            // requires scheduling on the next tick to avoid client desync.
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
            Bukkit.getScheduler().runTask(plugin, () -> openDetail(player, entry));
        }

        private ItemStack headerBook() {
            ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.index.header.name")
                        .style(noItalic()));
                meta.lore(splitLore(messages.plain("guide.index.header.lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderIndexEntry(GuideEntry entry) {
            ItemStack stack = new ItemStack(entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey()).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(entry.category().name(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                String descKey = descriptionKeyFor(entry.displayNameKey());
                if (descKey != null && messages.hasKey(descKey)) {
                    lore.add(Component.empty());
                    for (String line : messages.plain(descKey).split("\\r?\\n")) {
                        lore.add(Component.text(line, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                lore.add(Component.empty());
                lore.add(Component.text("» Click for recipe", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderLocked(GuideEntry entry) {
            ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.locked").style(noItalic()));
                meta.lore(List.of(
                        Component.text(entry.category().name(), NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    // --------------------------------------------------------------- detail

    private final class GuideDetailRenderer implements JavaInventoryRenderer<GuideEntry> {

        @Override public int size(@NotNull Player player, @NotNull GuideEntry ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull GuideEntry ctx) {
            return messages.component("guide.detail.title",
                    Placeholder.parsed("name", messages.plain(ctx.displayNameKey())));
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull GuideEntry entry) {
            // Border using a colour that matches the entry's category vibe.
            ItemStack border = decorativePane(Material.BLUE_STAINED_GLASS_PANE);
            for (int slot : INDEX_BORDER) {
                inventory.setItem(slot, border);
            }

            // Header icon: full description + category.
            inventory.setItem(DETAIL_ICON_SLOT, headerIcon(entry));

            // Recipe section.
            SapientiaRecipe recipe = findRecipeFor(entry.id());
            if (recipe != null) {
                List<RecipeIngredient> pattern = recipe.pattern();
                for (int i = 0; i < DETAIL_RECIPE_SLOTS.length && i < pattern.size(); i++) {
                    inventory.setItem(DETAIL_RECIPE_SLOTS[i], renderIngredient(pattern.get(i)));
                }
                inventory.setItem(DETAIL_ARROW_SLOT, arrow());
                inventory.setItem(DETAIL_RESULT_SLOT, renderResult(recipe));
            } else {
                // No recipe — fill the recipe area with a single explanatory item.
                ItemStack noRecipe = new ItemStack(Material.BARRIER);
                ItemMeta meta = noRecipe.getItemMeta();
                if (meta != null) {
                    meta.displayName(messages.component("guide.detail.recipe.header").style(noItalic()));
                    meta.lore(splitLore(messages.plain("guide.detail.recipe.none"), NamedTextColor.GRAY));
                    noRecipe.setItemMeta(meta);
                }
                inventory.setItem(DETAIL_RECIPE_SLOTS[4], noRecipe); // centre slot 29
            }

            // Back button.
            inventory.setItem(DETAIL_BACK_SLOT, backButton());
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull GuideEntry context, int slot) {
            if (slot != DETAIL_BACK_SLOT) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
        }

        private ItemStack headerIcon(GuideEntry entry) {
            ItemStack stack = new ItemStack(entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey()).style(noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(messages.component("guide.detail.category",
                                Placeholder.parsed("category", entry.category().name()))
                        .decoration(TextDecoration.ITALIC, false));
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
                case RecipeIngredient.Vanilla v -> {
                    ItemStack s = new ItemStack(v.material(), Math.max(1, v.amount()));
                    ItemMeta meta = s.getItemMeta();
                    if (meta != null) {
                        meta.lore(List.of(Component.text("Vanilla ingredient", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
                        s.setItemMeta(meta);
                    }
                    yield s;
                }
                case RecipeIngredient.Sapientia sap -> {
                    ItemStack s = Sapientia.get().createStack(sap.id(), Math.max(1, sap.amount())).orElse(null);
                    if (s == null) {
                        // Fallback display when the referenced item isn't registered.
                        s = new ItemStack(Material.BARRIER, Math.max(1, sap.amount()));
                    }
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

    // --------------------------------------------------------------- bedrock

    private final class GuideIndexBedrockRenderer
            implements dev.brmz.sapientia.api.ui.BedrockFormRenderer<Player> {

        @Override
        public void open(@NotNull Player player, @NotNull Player ctx) {
            List<GuideEntry> sorted = new ArrayList<>(entries.values());
            sorted.sort(Comparator.comparing((GuideEntry e) -> e.category().ordinal())
                    .thenComparing(e -> e.id().toString()));
            // Filter to entries the player has unlocked; locked ones are silently
            // omitted on Bedrock since SimpleForm has no "greyed out" state.
            List<GuideEntry> visible = new ArrayList<>();
            for (GuideEntry e : sorted) {
                if (e.discoveredByDefault() || unlockService.isUnlocked(player.getUniqueId(), e.id())) {
                    visible.add(e);
                }
            }

            String title = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.title"));
            String content = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.index.header.lore"));
            dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm form =
                    new dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm()
                            .title(title)
                            .content(content);
            for (GuideEntry e : visible) {
                form.button(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                        messages.component(e.displayNameKey())));
            }
            form.onClick(idx -> {
                if (idx < 0 || idx >= visible.size()) return;
                GuideEntry chosen = visible.get(idx);
                Bukkit.getScheduler().runTask(plugin, () -> openDetail(player, chosen));
            });
            form.send(player);
        }
    }

    private final class GuideDetailBedrockRenderer
            implements dev.brmz.sapientia.api.ui.BedrockFormRenderer<GuideEntry> {

        @Override
        public void open(@NotNull Player player, @NotNull GuideEntry entry) {
            String title = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.title",
                            Placeholder.parsed("name", messages.plain(entry.displayNameKey()))));

            StringBuilder body = new StringBuilder();
            body.append(dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.category",
                            Placeholder.parsed("category", entry.category().name()))))
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
            } else {
                body.append(messages.plain("guide.detail.recipe.none"));
            }

            String backLabel = dev.brmz.sapientia.core.i18n.TextAdapter.toPlainBedrock(
                    messages.component("guide.detail.back.name"));
            new dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm()
                    .title(title)
                    .content(body.toString())
                    .button(backLabel)
                    .onClick(idx -> Bukkit.getScheduler().runTask(plugin,
                            () -> GuideServiceImpl.this.open(player)))
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

    // --------------------------------------------------------------- helpers

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
            if (target.equals(tagged)) {
                return r;
            }
        }
        return null;
    }

    private static String descriptionKeyFor(String displayNameKey) {
        if (displayNameKey.endsWith(".name")) {
            return displayNameKey.substring(0, displayNameKey.length() - ".name".length()) + ".desc";
        }
        return null;
    }

    private static List<Component> splitLore(String raw, NamedTextColor color) {
        List<Component> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            out.add(Component.text(line, color).decoration(TextDecoration.ITALIC, false));
        }
        return out;
    }

    private static ItemStack decorativePane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Style noItalic() {
        return Style.style().decoration(TextDecoration.ITALIC, false).build();
    }

    /** Outer-perimeter slots of a 54-slot chest (rows 0 and 5, plus left/right edges). */
    private static int[] borderSlots54() {
        List<Integer> out = new ArrayList<>(28);
        for (int i = 0; i < 9; i++) out.add(i);
        for (int row = 1; row <= 4; row++) {
            out.add(row * 9);
            out.add(row * 9 + 8);
        }
        for (int i = 45; i < 54; i++) out.add(i);
        // Drop slot 4 (header) so the index renderer can occupy it.
        out.removeIf(s -> s == INDEX_HEADER_SLOT);
        // Drop slot 49 so the detail renderer's BACK button can sit there.
        out.removeIf(s -> s == DETAIL_BACK_SLOT);
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    /** Inner 7x4 area = rows 1..4, columns 1..7 (28 slots). */
    private static int[] innerSlots54() {
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
