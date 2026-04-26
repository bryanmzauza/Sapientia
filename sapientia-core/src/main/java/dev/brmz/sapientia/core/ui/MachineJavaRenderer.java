package dev.brmz.sapientia.core.ui;

import java.util.ArrayList;
import java.util.List;

import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.core.i18n.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Java 27-slot machine UI (T-145 / T-202).
 *
 * <p>Layout (slots, 0-indexed):
 * <pre>
 *   0  1  2  3  4  5  6  7  8     ─ border
 *   9 10 11 12[E]14 15[T]17       ─ {@code 13}=energy bar, {@code 16}=toggle
 *  18 19 20 21 22 23 24 25 26     ─ border
 * </pre>
 * The "energy bar" is an XP-bottle whose stack-amount approximates the buffer
 * fill level (1..64). The toggle right-side is a lever / barrier depending on
 * running flag and refreshes the inventory on click.
 */
public final class MachineJavaRenderer implements JavaInventoryRenderer<EnergyNode> {

    private static final int ENERGY_SLOT = 13;
    private static final int TOGGLE_SLOT = 16;

    private final Messages messages;
    private final MachineRunningRegistry running;

    public MachineJavaRenderer(@NotNull Messages messages,
                                @NotNull MachineRunningRegistry running) {
        this.messages = messages;
        this.running = running;
    }

    @Override
    public int size(@NotNull Player player, @NotNull EnergyNode context) {
        return 27;
    }

    @Override
    public @NotNull Component title(@NotNull Player player, @NotNull EnergyNode context) {
        return messages.component("ui.machine.title");
    }

    @Override
    public void render(@NotNull Inventory inventory, @NotNull Player player, @NotNull EnergyNode node) {
        ItemStack pane = decorativePane();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }
        inventory.setItem(ENERGY_SLOT, energyBar(node));
        inventory.setItem(TOGGLE_SLOT, runningToggle(node));
    }

    @Override
    public void onClick(@NotNull Player player, @NotNull EnergyNode node, int slot) {
        if (slot != TOGGLE_SLOT) {
            return;
        }
        boolean wasRunning = running.isRunning(node);
        running.setRunning(node, !wasRunning);
        player.playSound(player.getLocation(),
                wasRunning ? Sound.UI_BUTTON_CLICK : Sound.BLOCK_LEVER_CLICK,
                0.6f, wasRunning ? 0.8f : 1.6f);
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv.getSize() == 27) {
            inv.setItem(TOGGLE_SLOT, runningToggle(node));
            inv.setItem(ENERGY_SLOT, energyBar(node));
        }
    }

    private ItemStack energyBar(EnergyNode node) {
        long curr = node.bufferCurrent();
        long max = Math.max(1, node.bufferMax());
        int amount = (int) Math.max(1, Math.min(64, Math.round((curr * 64.0) / (double) max)));
        int percent = (int) Math.round((curr * 100.0) / (double) max);
        ItemStack stack = new ItemStack(Material.EXPERIENCE_BOTTLE, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.component("ui.machine.energy.title").style(noItalic()));
            List<Component> lore = new ArrayList<>();
            lore.add(messages.component("ui.machine.energy.value",
                            Placeholder.parsed("current", Long.toString(curr)),
                            Placeholder.parsed("max", Long.toString(node.bufferMax())))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(messages.component("ui.machine.energy.percent",
                            Placeholder.parsed("percent", Integer.toString(percent)))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack runningToggle(EnergyNode node) {
        boolean isRunning = running.isRunning(node);
        Material mat = isRunning ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String key = isRunning ? "ui.machine.running" : "ui.machine.stopped";
            meta.displayName(messages.component(key).style(noItalic()));
            meta.lore(List.of(
                    messages.component(isRunning
                                    ? "ui.machine.click.stop"
                                    : "ui.machine.click.start")
                            .decoration(TextDecoration.ITALIC, false)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack decorativePane() {
        ItemStack stack = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Style noItalic() {
        return Style.style().decoration(TextDecoration.ITALIC, false).build();
    }
}
