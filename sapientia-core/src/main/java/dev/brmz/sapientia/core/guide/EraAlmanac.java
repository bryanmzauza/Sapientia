package dev.brmz.sapientia.core.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.guide.GuideEntry;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.i18n.TextAdapter;
import dev.brmz.sapientia.core.progression.ProgressionServiceImpl;
import dev.brmz.sapientia.core.ui.UIService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The era almanac: every era, and for each one every item, block and machine it
 * brings. Eras not unlocked yet list their entries as barriers with their
 * names; entries of unlocked eras open the guide's detail page. Like the
 * guide, it reopens on the view the player left it.
 */
final class EraAlmanac {

    static final NamespacedKey INDEX_KEY = NamespacedKey.fromString("sapientia:era_almanac");
    static final NamespacedKey ERA_KEY = NamespacedKey.fromString("sapientia:era_almanac_era");

    private static final NamespacedKey ALMANAC_ITEM = NamespacedKey.fromString("sapientia:era_almanac");
    private static final int HEADER_SLOT = 4;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int[] INNER_SLOTS = GuideServiceImpl.innerSlots54();
    private static final int PAGE_SIZE = INNER_SLOTS.length;

    private final GuideServiceImpl guide;
    private final Plugin plugin;
    private final UIService uiService;
    private final ProgressionServiceImpl progression;
    private final Messages messages;
    private final NamespacedKey viewKey;

    EraAlmanac(@NotNull GuideServiceImpl guide, @NotNull Plugin plugin, @NotNull UIService uiService,
               @NotNull ProgressionServiceImpl progression, @NotNull Messages messages) {
        this.guide = guide;
        this.plugin = plugin;
        this.uiService = uiService;
        this.progression = progression;
        this.messages = messages;
        this.viewKey = new NamespacedKey(plugin, "eras_view");
        uiService.register(new IndexDescriptor());
        uiService.register(new EraDescriptor());
    }

    /** Reopens the view the player left the almanac on, or the era list. */
    void open(@NotNull Player player) {
        String saved = player.getPersistentDataContainer().get(viewKey, PersistentDataType.STRING);
        String[] parts = saved == null ? new String[0] : saved.split("\\|");
        if (parts.length == 3 && parts[0].equals("era")) {
            openEra(player, new EraView(Era.of(GuideServiceImpl.parseInt(parts[1])), GuideServiceImpl.parseInt(parts[2])));
            return;
        }
        if (parts.length == 4 && parts[0].equals("entry")) {
            EraView from = new EraView(Era.of(GuideServiceImpl.parseInt(parts[2])), GuideServiceImpl.parseInt(parts[3]));
            NamespacedKey id = NamespacedKey.fromString(parts[1]);
            GuideEntry entry = id == null ? null : guide.entries.get(id);
            if (entry != null && !guide.isEraLocked(entry, player)) {
                openEntry(player, entry, from);
            } else {
                openEra(player, from);
            }
            return;
        }
        openIndex(player);
    }

    private void openIndex(@NotNull Player player) {
        remember(player, "index");
        uiService.open(player, new IndexDescriptor(), player);
    }

    private void openEra(@NotNull Player player, @NotNull EraView view) {
        remember(player, "era|" + view.era().number() + "|" + view.page());
        uiService.open(player, new EraDescriptor(), view);
    }

    private void openEntry(@NotNull Player player, @NotNull GuideEntry entry, @NotNull EraView from) {
        remember(player, "entry|" + entry.id() + "|" + from.era().number() + "|" + from.page());
        guide.openDetail(player, entry, p -> openEra(p, from));
    }

    private void remember(@NotNull Player player, @NotNull String view) {
        player.getPersistentDataContainer().set(viewKey, PersistentDataType.STRING, view);
    }

    /** Entries of one era, by category and then id. */
    private List<GuideEntry> entriesOf(@NotNull Era era) {
        List<GuideEntry> list = new ArrayList<>();
        for (GuideEntry entry : guide.entries.values()) {
            if (progression.eraOf(entry.id()) == era) list.add(entry);
        }
        list.sort(Comparator.comparing((GuideEntry e) -> e.category().ordinal())
                .thenComparing(e -> e.id().toString()));
        return list;
    }

    /** Number of entries of each era, in one pass. */
    private Map<Era, Integer> countsByEra() {
        Map<Era, Integer> counts = new EnumMap<>(Era.class);
        for (GuideEntry entry : guide.entries.values()) {
            counts.merge(progression.eraOf(entry.id()), 1, Integer::sum);
        }
        return counts;
    }

    private boolean isOpen(@NotNull Era era, @NotNull Player player) {
        return progression.isUnlocked(era) || progression.bypasses(player);
    }

    private Component eraName(@NotNull Era era) {
        return messages.component("almanac.era.name",
                Placeholder.unparsed("number", Integer.toString(era.number())),
                Placeholder.component("era", messages.component(era.nameKey())));
    }

    private static Component line(@NotNull Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /** Context for one era's paginated entry list. */
    private record EraView(Era era, int page) {}

    // -- descriptors -----------------------------------------------------------

    private final class IndexDescriptor implements UIDescriptor<Player> {
        @Override public @NotNull NamespacedKey key() { return INDEX_KEY; }
        @Override public @NotNull JavaInventoryRenderer<Player> javaRenderer() { return new IndexRenderer(); }
        @Override public BedrockFormRenderer<Player> bedrockRenderer() { return new IndexBedrockRenderer(); }
    }

    private final class EraDescriptor implements UIDescriptor<EraView> {
        @Override public @NotNull NamespacedKey key() { return ERA_KEY; }
        @Override public @NotNull JavaInventoryRenderer<EraView> javaRenderer() { return new EraRenderer(); }
        @Override public BedrockFormRenderer<EraView> bedrockRenderer() { return new EraBedrockRenderer(); }
    }

    // -- java ------------------------------------------------------------------

    private final class IndexRenderer implements JavaInventoryRenderer<Player> {

        private final Map<Integer, Era> slotIndex = new HashMap<>();

        @Override public int size(@NotNull Player player, @NotNull Player ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull Player ctx) {
            return messages.component("almanac.title");
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull Player ctx) {
            slotIndex.clear();
            ItemStack border = GuideServiceImpl.decorativePane(Material.BLACK_STAINED_GLASS_PANE);
            for (int slot : GuideServiceImpl.CHEST_BORDER) inventory.setItem(slot, border);
            inventory.setItem(HEADER_SLOT, header(player));

            Map<Era, Integer> counts = countsByEra();
            Era[] eras = Era.values();
            for (int i = 0; i < eras.length && i < INNER_SLOTS.length; i++) {
                Era era = eras[i];
                inventory.setItem(INNER_SLOTS[i], eraButton(era, player, counts.getOrDefault(era, 0), true));
                slotIndex.put(INNER_SLOTS[i], era);
            }
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull Player context, int slot) {
            Era era = slotIndex.get(slot);
            if (era == null) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
            Bukkit.getScheduler().runTask(plugin, () -> openEra(player, new EraView(era, 0)));
        }

        private ItemStack header(Player player) {
            ItemStack stack = GuideServiceImpl.sapientiaIcon(ALMANAC_ITEM, Material.KNOWLEDGE_BOOK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("almanac.header.name").style(GuideServiceImpl.noItalic()));
                List<Component> lore = new ArrayList<>(GuideServiceImpl.splitLore(
                        messages.plain("almanac.header.lore"), NamedTextColor.GRAY));
                lore.add(Component.empty());
                for (Component l : guide.progressLines(player)) lore.add(line(l));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    /** An era's button: its gateway item when unlocked, a barrier when not. */
    private ItemStack eraButton(Era era, Player player, int count, boolean clickHint) {
        boolean open = isOpen(era, player);
        NamespacedKey gateway = era.gatewayItem();
        ItemStack stack = !open ? new ItemStack(Material.BARRIER)
                : gateway != null ? GuideServiceImpl.sapientiaIcon(gateway, Material.BOOK)
                : new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(eraName(era).style(GuideServiceImpl.noItalic()));
            List<Component> lore = new ArrayList<>(GuideServiceImpl.splitLore(
                    messages.plain(era.summaryKey()), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(line(messages.component("guide.index.button.count",
                    Placeholder.parsed("count", Integer.toString(count)))));
            if (era == progression.serverEra()) {
                lore.add(line(messages.component("almanac.era.current")));
            } else if (!progression.isUnlocked(era)) {
                lore.add(line(messages.component("almanac.era.locked")));
            }
            if (clickHint) lore.add(line(messages.component("almanac.era.click")));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private final class EraRenderer implements JavaInventoryRenderer<EraView> {

        private final Map<Integer, GuideEntry> slotIndex = new HashMap<>();
        private boolean hasPrev;
        private boolean hasNext;
        private int shownPage;

        @Override public int size(@NotNull Player player, @NotNull EraView ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull EraView ctx) {
            return messages.component("almanac.era.title",
                    Placeholder.unparsed("number", Integer.toString(ctx.era().number())));
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull EraView view) {
            slotIndex.clear();
            ItemStack border = GuideServiceImpl.decorativePane(Material.BLACK_STAINED_GLASS_PANE);
            for (int slot : GuideServiceImpl.CHEST_BORDER) inventory.setItem(slot, border);

            List<GuideEntry> all = entriesOf(view.era());
            int totalPages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            shownPage = Math.max(0, Math.min(view.page(), totalPages - 1));
            ItemStack header = eraButton(view.era(), player, all.size(), false);
            ItemMeta headerMeta = header.getItemMeta();
            if (headerMeta != null && headerMeta.lore() != null) {
                List<Component> lore = new ArrayList<>(headerMeta.lore());
                lore.add(line(messages.component("guide.page.indicator",
                        Placeholder.parsed("page", Integer.toString(shownPage + 1)),
                        Placeholder.parsed("total", Integer.toString(totalPages)))));
                headerMeta.lore(lore);
                header.setItemMeta(headerMeta);
            }
            inventory.setItem(HEADER_SLOT, header);

            int from = shownPage * PAGE_SIZE;
            int to = Math.min(all.size(), from + PAGE_SIZE);
            for (int i = from, idx = 0; i < to; i++, idx++) {
                GuideEntry entry = all.get(i);
                if (guide.isEraLocked(entry, player)) {
                    inventory.setItem(INNER_SLOTS[idx], lockedEntry(entry));
                } else {
                    inventory.setItem(INNER_SLOTS[idx], entryIcon(entry, player));
                    slotIndex.put(INNER_SLOTS[idx], entry);
                }
            }

            hasPrev = shownPage > 0;
            hasNext = shownPage < totalPages - 1;
            inventory.setItem(PREV_SLOT, hasPrev ? button(Material.ARROW, "guide.page.prev")
                    : GuideServiceImpl.decorativePane(Material.GRAY_STAINED_GLASS_PANE));
            inventory.setItem(NEXT_SLOT, hasNext ? button(Material.ARROW, "guide.page.next")
                    : GuideServiceImpl.decorativePane(Material.GRAY_STAINED_GLASS_PANE));
            inventory.setItem(BACK_SLOT, button(Material.SPECTRAL_ARROW, "almanac.back"));
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull EraView ctx, int slot) {
            if (slot == BACK_SLOT) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
                Bukkit.getScheduler().runTask(plugin, () -> openIndex(player));
                return;
            }
            if ((slot == PREV_SLOT && hasPrev) || (slot == NEXT_SLOT && hasNext)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
                EraView next = new EraView(ctx.era(), shownPage + (slot == PREV_SLOT ? -1 : 1));
                Bukkit.getScheduler().runTask(plugin, () -> openEra(player, next));
                return;
            }
            GuideEntry entry = slotIndex.get(slot);
            if (entry == null) return;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
            EraView from = new EraView(ctx.era(), shownPage);
            Bukkit.getScheduler().runTask(plugin, () -> openEntry(player, entry, from));
        }

        private ItemStack entryIcon(GuideEntry entry, Player player) {
            ItemStack stack = GuideServiceImpl.sapientiaIcon(entry.id(), entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey()).style(GuideServiceImpl.noItalic()));
                List<Component> lore = new ArrayList<>();
                lore.add(line(categoryLine(entry)));
                lore.add(line(messages.component(guide.isVisible(entry, player)
                        ? "almanac.entry.known" : "almanac.entry.unknown")));
                lore.add(Component.empty());
                lore.add(line(messages.component("guide.entry.click")));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack lockedEntry(GuideEntry entry) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.locked-era.name",
                                Placeholder.component("name", messages.component(entry.displayNameKey())))
                        .style(GuideServiceImpl.noItalic()));
                meta.lore(List.of(line(categoryLine(entry)),
                        line(messages.component("guide.locked-era.lore"))));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private Component categoryLine(GuideEntry entry) {
            return messages.component("guide.detail.category", Placeholder.parsed("category",
                    messages.plain(GuideServiceImpl.categoryNameKey(entry.category()))));
        }

        private ItemStack button(Material material, String key) {
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(key + ".name").style(GuideServiceImpl.noItalic()));
                meta.lore(GuideServiceImpl.splitLore(messages.plain(key + ".lore"), NamedTextColor.GRAY));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }

    // -- bedrock ---------------------------------------------------------------

    private final class IndexBedrockRenderer implements BedrockFormRenderer<Player> {

        @Override
        public void open(@NotNull Player player, @NotNull Player ctx) {
            StringBuilder content = new StringBuilder(messages.plain("almanac.header.lore")).append('\n');
            for (Component l : guide.progressLines(player)) {
                content.append('\n').append(TextAdapter.toPlainBedrock(l));
            }
            SapientiaSimpleForm form = new SapientiaSimpleForm()
                    .title(TextAdapter.toPlainBedrock(messages.component("almanac.title")))
                    .content(content.toString());
            Era[] eras = Era.values();
            for (Era era : eras) {
                form.button(TextAdapter.toPlainBedrock(messages.component(
                        isOpen(era, player) ? "almanac.era.bedrock" : "almanac.era.bedrock-locked",
                        Placeholder.unparsed("number", Integer.toString(era.number())),
                        Placeholder.component("era", messages.component(era.nameKey())))));
            }
            form.onClick(idx -> {
                if (idx < 0 || idx >= eras.length) return;
                Era chosen = eras[idx];
                Bukkit.getScheduler().runTask(plugin, () -> openEra(player, new EraView(chosen, 0)));
            });
            form.send(player);
        }
    }

    private final class EraBedrockRenderer implements BedrockFormRenderer<EraView> {

        @Override
        public void open(@NotNull Player player, @NotNull EraView view) {
            List<GuideEntry> all = entriesOf(view.era());
            StringBuilder content = new StringBuilder(messages.plain(view.era().summaryKey()))
                    .append("\n\n")
                    .append(TextAdapter.toPlainBedrock(messages.component("guide.index.button.count",
                            Placeholder.parsed("count", Integer.toString(all.size())))));
            if (!progression.isUnlocked(view.era())) {
                content.append('\n').append(TextAdapter.toPlainBedrock(messages.component("almanac.era.locked")));
            }
            SapientiaSimpleForm form = new SapientiaSimpleForm()
                    .title(TextAdapter.toPlainBedrock(eraName(view.era())))
                    .content(content.toString());
            for (GuideEntry entry : all) {
                Component label = guide.isEraLocked(entry, player)
                        ? messages.component("guide.locked-era.bedrock",
                                Placeholder.component("name", messages.component(entry.displayNameKey())),
                                Placeholder.unparsed("number", Integer.toString(view.era().number())))
                        : messages.component(entry.displayNameKey());
                form.button(TextAdapter.toPlainBedrock(label));
            }
            form.button(TextAdapter.toPlainBedrock(messages.component("almanac.back.name")));
            form.onClick(idx -> {
                if (idx < 0) return;
                if (idx >= all.size()) {
                    Bukkit.getScheduler().runTask(plugin, () -> openIndex(player));
                    return;
                }
                GuideEntry chosen = all.get(idx);
                if (guide.isEraLocked(chosen, player)) {
                    Bukkit.getScheduler().runTask(plugin, () -> openEra(player, view));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> openEntry(player, chosen, view));
                }
            });
            form.send(player);
        }
    }
}
