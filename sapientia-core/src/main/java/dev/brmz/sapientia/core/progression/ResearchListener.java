package dev.brmz.sapientia.core.progression;

import java.util.function.Function;

import dev.brmz.sapientia.api.events.SapientiaDiscoveryEvent;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Player research: loads discoveries on join, records items the player picks
 * up or takes out of containers (crafting and harvests go through those too),
 * and tells the player when a discovery unlocks recipes.
 */
public final class ResearchListener implements Listener {

    private final ProgressionServiceImpl progression;
    private final ItemRegistry items;
    private final Messages messages;
    private final Function<NamespacedKey, String> recipeNameKey;

    /**
     * @param recipeNameKey display name key of a recipe's result, or {@code null}
     */
    public ResearchListener(@NotNull ProgressionServiceImpl progression, @NotNull ItemRegistry items,
                            @NotNull Messages messages, @NotNull Function<NamespacedKey, String> recipeNameKey) {
        this.progression = progression;
        this.items = items;
        this.messages = messages;
        this.recipeNameKey = recipeNameKey;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Items already carried count as discovered (players from before research existed).
        progression.load(player.getUniqueId(), () -> {
            discoverAll(player, player.getInventory());
            discoverAll(player, player.getEnderChest());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        progression.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        discover(event.getPlayer(), event.getItemInHand()); // for blocks obtained without being picked up
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            discover(player, event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTake(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked == player.getInventory()) return;
        // Plugin screens (guide, machine UIs, workbench) show items without handing them out.
        if (dev.brmz.sapientia.core.ui.UIService.isPluginScreen(clicked.getHolder())
                || clicked.getHolder() instanceof dev.brmz.sapientia.core.crafting.WorkbenchHolder) return;
        discover(player, event.getCurrentItem());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDiscovery(@NotNull SapientiaDiscoveryEvent event) {
        if (event.unlockedRecipes().isEmpty()) return;
        Player player = Bukkit.getPlayer(event.player());
        if (player == null) return;
        for (NamespacedKey recipe : event.unlockedRecipes()) {
            String key = recipeNameKey.apply(recipe);
            Component name = key != null ? messages.component(key) : Component.text(recipe.getKey());
            player.sendMessage(messages.component("research.unlocked", Placeholder.component("recipe", name)));
        }
    }

    private void discoverAll(Player player, Inventory inventory) {
        for (ItemStack stack : inventory.getContents()) {
            discover(player, stack);
        }
    }

    private void discover(Player player, @Nullable ItemStack stack) {
        String id = items.idOf(stack);
        if (id == null) return;
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key != null) progression.discover(player.getUniqueId(), key);
    }
}
