package dev.brmz.sapientia.core.ui;

import java.util.List;
import java.util.Locale;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.logistics.ItemFilterMode;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.api.logistics.ItemNetwork;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemRoutingPolicy;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.bedrock.forms.SapientiaCustomForm;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.i18n.TextAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item filter UI (T-300 / 1.1.0). Right-clicking a {@code SapientiaItemFilter}
 * opens this descriptor with the underlying {@link ItemNode} as context.
 *
 * <p>The Java renderer is read-only: it shows the filter's current rules and a
 * banner with the network's routing policy. Rule editing happens via
 * {@code /sapientia logistics filter add|remove|clear}. Bedrock players see an
 * equivalent {@link SapientiaCustomForm} summary.
 *
 * <p>The 1.0.0 experimental stub (Player-context, gated by
 * {@code experimental.filter}) is replaced by this fully wired version.
 */
public final class FilterDescriptor implements UIDescriptor<ItemNode> {

    public static final NamespacedKey KEY = NamespacedKey.fromString("sapientia:filter");

    private final Messages messages;

    public FilterDescriptor(@NotNull Messages messages) {
        this.messages = messages;
    }

    @Override
    public @NotNull NamespacedKey key() {
        return KEY;
    }

    @Override
    public @NotNull JavaInventoryRenderer<ItemNode> javaRenderer() {
        return new FilterJavaRenderer();
    }

    @Override
    public @Nullable BedrockFormRenderer<ItemNode> bedrockRenderer() {
        return new FilterBedrockRenderer();
    }

    private final class FilterJavaRenderer implements JavaInventoryRenderer<ItemNode> {
        @Override public int size(@NotNull Player p, @NotNull ItemNode ctx) { return 27; }

        @Override public @NotNull Component title(@NotNull Player p, @NotNull ItemNode ctx) {
            return messages.component("ui.filter.title");
        }

        @Override
        public void render(@NotNull Inventory inv, @NotNull Player p, @NotNull ItemNode ctx) {
            List<ItemFilterRule> rules = Sapientia.get().logistics().getFilterRules(ctx.nodeId());
            for (int i = 0; i < Math.min(rules.size(), 9); i++) {
                ItemFilterRule rule = rules.get(i);
                Material mat = rule.mode() == ItemFilterMode.BLACKLIST
                        ? Material.RED_STAINED_GLASS_PANE
                        : Material.LIME_STAINED_GLASS_PANE;
                ItemStack ticket = new ItemStack(mat);
                ItemMeta meta = ticket.getItemMeta();
                if (meta != null) {
                    meta.displayName(messages.component("ui.filter.rule.line",
                            Placeholder.parsed("mode", rule.mode().name()),
                            Placeholder.parsed("pattern", rule.pattern())));
                    ticket.setItemMeta(meta);
                }
                inv.setItem(i, ticket);
            }
            if (rules.isEmpty()) {
                ItemStack empty = new ItemStack(Material.PAPER);
                ItemMeta meta = empty.getItemMeta();
                if (meta != null) {
                    meta.displayName(messages.component("ui.filter.empty"));
                    empty.setItemMeta(meta);
                }
                inv.setItem(4, empty);
            }
            ItemNetwork network = Sapientia.get().logistics().networkOf(ctx).orElse(null);
            ItemRoutingPolicy policy = network == null ? ItemRoutingPolicy.ROUND_ROBIN : network.routingPolicy();
            ItemStack banner = new ItemStack(Material.COMPASS);
            ItemMeta bmeta = banner.getItemMeta();
            if (bmeta != null) {
                bmeta.displayName(messages.component("ui.filter.policy",
                        Placeholder.parsed("policy", policy.name())));
                bmeta.lore(List.of(messages.component("ui.filter.help")));
                banner.setItemMeta(bmeta);
            }
            inv.setItem(13, banner);
        }
    }

    private final class FilterBedrockRenderer implements BedrockFormRenderer<ItemNode> {
        @Override
        public void open(@NotNull Player player, @NotNull ItemNode ctx) {
            List<ItemFilterRule> rules = Sapientia.get().logistics().getFilterRules(ctx.nodeId());
            ItemNetwork network = Sapientia.get().logistics().networkOf(ctx).orElse(null);
            ItemRoutingPolicy policy = network == null ? ItemRoutingPolicy.ROUND_ROBIN : network.routingPolicy();

            SapientiaCustomForm form = new SapientiaCustomForm()
                    .title(TextAdapter.toPlainBedrock(messages.component("ui.filter.title")))
                    .label(TextAdapter.toPlainBedrock(messages.component(
                            "ui.filter.policy",
                            Placeholder.parsed("policy", policy.name()))));
            if (rules.isEmpty()) {
                form.label(TextAdapter.toPlainBedrock(messages.component("ui.filter.empty")));
            } else {
                for (ItemFilterRule rule : rules) {
                    form.label(TextAdapter.toPlainBedrock(messages.component(
                            "ui.filter.rule.line",
                            Placeholder.parsed("mode", rule.mode().name().toLowerCase(Locale.ROOT)),
                            Placeholder.parsed("pattern", rule.pattern()))));
                }
            }
            form.label(TextAdapter.toPlainBedrock(messages.component("ui.filter.help")));
            form.send(player);
        }
    }
}
