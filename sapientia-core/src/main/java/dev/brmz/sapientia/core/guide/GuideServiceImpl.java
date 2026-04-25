package dev.brmz.sapientia.core.guide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link GuideService} implementation (T-150 / 0.4.0). Auto-populates
 * entries from the content registries at start-up and renders them as a 54-slot
 * chest grouped by {@link GuideCategory}. Locked entries (those not in the
 * player's {@link UnlockService} set) render as grey panes with a hint.
 */
public final class GuideServiceImpl implements GuideService {

    static final NamespacedKey DESCRIPTOR_KEY = NamespacedKey.fromString("sapientia:guide");

    private final Map<NamespacedKey, GuideEntry> entries = new LinkedHashMap<>();
    private final UIService uiService;
    private final UnlockService unlockService;
    private final Messages messages;

    public GuideServiceImpl(
            @NotNull Plugin plugin,
            @NotNull UIService uiService,
            @NotNull UnlockService unlockService,
            @NotNull Messages messages) {
        this.uiService = uiService;
        this.unlockService = unlockService;
        this.messages = messages;
        uiService.register(new GuideDescriptor());
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
        uiService.open(player, new GuideDescriptor(), player);
    }

    // --------------------------------------------------------------- rendering

    private final class GuideDescriptor implements UIDescriptor<Player> {
        @Override public @NotNull NamespacedKey key() { return DESCRIPTOR_KEY; }
        @Override public @NotNull JavaInventoryRenderer<Player> javaRenderer() { return new GuideRenderer(); }
        @Override public dev.brmz.sapientia.api.ui.BedrockFormRenderer<Player> bedrockRenderer() { return null; }
    }

    private final class GuideRenderer implements JavaInventoryRenderer<Player> {
        @Override public int size(@NotNull Player player, @NotNull Player ctx) { return 54; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull Player ctx) {
            return messages.component("guide.title");
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull Player ctx) {
            List<GuideEntry> sorted = new ArrayList<>(entries.values());
            sorted.sort(Comparator.comparing((GuideEntry e) -> e.category().ordinal())
                    .thenComparing(e -> e.id().toString()));
            int slot = 0;
            for (GuideEntry entry : sorted) {
                if (slot >= 54) break;
                boolean unlocked = entry.discoveredByDefault()
                        || unlockService.isUnlocked(player.getUniqueId(), entry.id());
                inventory.setItem(slot++, unlocked ? renderEntry(entry) : renderLocked(entry));
            }
        }

        private ItemStack renderEntry(GuideEntry entry) {
            ItemStack stack = new ItemStack(entry.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component(entry.displayNameKey())
                        .style(Style.style().decoration(TextDecoration.ITALIC, false).build()));
                meta.lore(List.of(
                        Component.text(entry.category().name(), NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private ItemStack renderLocked(GuideEntry entry) {
            ItemStack stack = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("guide.locked")
                        .style(Style.style().decoration(TextDecoration.ITALIC, false).build()));
                meta.lore(List.of(
                        Component.text(entry.category().name(), NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
                stack.setItemMeta(meta);
            }
            return stack;
        }
    }
}
