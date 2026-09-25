package dev.brmz.sapientia.core.progression;

import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.api.progression.ProgressionService;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Era locks outside the workbench and machines:
 * <ul>
 *   <li>Sapientia items of a locked era cannot be placed (blocks, seeds or
 *       items backed by placeable vanilla blocks);</li>
 *   <li>Sapientia items never act as their vanilla base material in vanilla
 *       crafting, smelting, the crafter or the smithing table, so for
 *       example a copper ingot of the Sapientia cannot make iron tools.</li>
 * </ul>
 * Players with {@code sapientia.era.bypass} ignore the placement lock.
 */
public final class EraLockListener implements Listener {

    private final ProgressionService progression;
    private final ItemRegistry items;
    private final Messages messages;

    public EraLockListener(@NotNull ProgressionService progression, @NotNull ItemRegistry items,
                           @NotNull Messages messages) {
        this.progression = progression;
        this.items = items;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        NamespacedKey id = sapientiaId(event.getItemInHand());
        if (id == null) return;
        Era era = progression.eraOf(id);
        Player player = event.getPlayer();
        if (progression.isUnlocked(era) || progression.bypasses(player)) return;
        event.setCancelled(true);
        player.sendMessage(messages.component("era.locked.place",
                Placeholder.unparsed("number", Integer.toString(era.number())),
                Placeholder.component("era", messages.component(era.nameKey()))));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(@NotNull PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (sapientiaId(ingredient) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
        // Sapientia items made on the vanilla crafting table follow the era locks too.
        NamespacedKey result = sapientiaId(event.getInventory().getResult());
        if (result != null && !progression.isAvailable(result)
                && !(event.getView().getPlayer() instanceof Player player && progression.bypasses(player))) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(@NotNull CrafterCraftEvent event) {
        if (event.getBlock().getState() instanceof org.bukkit.block.Crafter crafter) {
            for (ItemStack ingredient : crafter.getInventory().getContents()) {
                if (sapientiaId(ingredient) != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmelt(@NotNull FurnaceSmeltEvent event) {
        if (sapientiaId(event.getSource()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCook(@NotNull BlockCookEvent event) {
        if (!(event instanceof FurnaceSmeltEvent) && sapientiaId(event.getSource()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithing(@NotNull PrepareSmithingEvent event) {
        for (ItemStack stack : event.getInventory().getContents()) {
            if (sapientiaId(stack) != null) {
                event.setResult(null);
                return;
            }
        }
    }

    private @Nullable NamespacedKey sapientiaId(@Nullable ItemStack stack) {
        String id = items.idOf(stack);
        return id == null ? null : NamespacedKey.fromString(id);
    }
}
