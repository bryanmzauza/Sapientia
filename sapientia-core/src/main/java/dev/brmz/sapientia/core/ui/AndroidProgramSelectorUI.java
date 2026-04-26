package dev.brmz.sapientia.core.ui;

import java.util.ArrayList;
import java.util.List;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.android.AndroidNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.core.i18n.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Android program selector UI (T-453 / 1.9.1).
 *
 * <p>Right-clicking a placed android opens a 27-slot chest listing every
 * registered {@link LogicProgram} as a paper ticket; clicking a ticket
 * assigns the program to the android via
 * {@code AndroidService#assignProgram} and closes the inventory. Slot 26 is
 * a barrier "clear" button that detaches the current program.
 *
 * <p>Bedrock players: no custom form is shipped — Sapientia's
 * {@code BedrockFormsUIProvider} (T-302l) auto-generates a flat-list form
 * from the Java layout (see docs/ui-strategy.md §3.2). Addons that want a
 * richer DAG editor canvas can still register their own UI under a
 * different key.
 */
public final class AndroidProgramSelectorUI implements UIDescriptor<AndroidNode> {

    public static final NamespacedKey KEY =
            NamespacedKey.fromString("sapientia:android_program_selector");

    private static final int CLEAR_SLOT = 26;
    private static final int MAX_PROGRAM_SLOTS = 26;

    private final Messages messages;

    public AndroidProgramSelectorUI(@NotNull Messages messages) {
        this.messages = messages;
    }

    @Override public @NotNull NamespacedKey key() { return KEY; }
    @Override public @NotNull JavaInventoryRenderer<AndroidNode> javaRenderer() { return new Renderer(messages); }
    @Override public @Nullable BedrockFormRenderer<AndroidNode> bedrockRenderer() { return null; }

    /**
     * Programs are looked up at render-time so the list always reflects the
     * latest {@code LogicService} state (no stale caches across reloads).
     */
    private static final class Renderer implements JavaInventoryRenderer<AndroidNode> {

        private final Messages messages;

        Renderer(@NotNull Messages messages) { this.messages = messages; }

        @Override
        public int size(@NotNull Player player, @NotNull AndroidNode context) { return 27; }

        @Override
        public @NotNull Component title(@NotNull Player player, @NotNull AndroidNode context) {
            return messages.component("ui.android.selector.title");
        }

        @Override
        public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull AndroidNode node) {
            // Background fill so the layout reads as a panel rather than
            // a half-empty chest.
            ItemStack pane = decorativePane();
            for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);

            List<String> names = new ArrayList<>(Sapientia.get().logic().list());
            int max = Math.min(names.size(), MAX_PROGRAM_SLOTS);
            for (int i = 0; i < max; i++) {
                String name = names.get(i);
                inventory.setItem(i, ticket(node, name));
            }
            inventory.setItem(CLEAR_SLOT, clearButton(node));
        }

        @Override
        public void onClick(@NotNull Player player, @NotNull AndroidNode node, int slot) {
            if (slot == CLEAR_SLOT) {
                Sapientia.get().androids().clearProgram(node.block());
                player.sendMessage(messages.component("ui.android.selector.cleared"));
                player.closeInventory();
                return;
            }
            if (slot < 0 || slot >= MAX_PROGRAM_SLOTS) return;
            List<String> names = new ArrayList<>(Sapientia.get().logic().list());
            if (slot >= names.size()) return;
            String name = names.get(slot);
            boolean ok = Sapientia.get().androids().assignProgram(node.block(), name);
            if (ok) {
                player.sendMessage(messages.component("ui.android.selector.assigned",
                        Placeholder.parsed("program", name)));
            } else {
                player.sendMessage(messages.component("ui.android.selector.failed",
                        Placeholder.parsed("program", name)));
            }
            player.closeInventory();
        }

        private @NotNull ItemStack ticket(@NotNull AndroidNode node, @NotNull String name) {
            LogicProgram prog = Sapientia.get().logic().get(name).orElse(null);
            ItemStack stack = new ItemStack(Material.PAPER);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return stack;
            boolean current = node.programName().filter(name::equals).isPresent();
            meta.displayName(messages.component("ui.android.selector.program",
                    Placeholder.parsed("program", name)).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            int nodes = prog == null ? 0 : prog.nodes().size();
            int edges = prog == null ? 0 : prog.edges().size();
            lore.add(messages.component("ui.android.selector.nodes",
                            Placeholder.parsed("count", Integer.toString(nodes)))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(messages.component("ui.android.selector.edges",
                            Placeholder.parsed("count", Integer.toString(edges)))
                    .decoration(TextDecoration.ITALIC, false));
            if (current) {
                lore.add(messages.component("ui.android.selector.current")
                        .decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
            return stack;
        }

        private @NotNull ItemStack clearButton(@NotNull AndroidNode node) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("ui.android.selector.clear")
                        .decoration(TextDecoration.ITALIC, false));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        private static @NotNull ItemStack decorativePane() {
            ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(" "));
                pane.setItemMeta(meta);
            }
            // Avoid Bukkit warning when called pre-server-init in tests.
            try { Bukkit.getServer(); } catch (Throwable ignored) {}
            return pane;
        }
    }
}
