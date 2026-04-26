package dev.brmz.sapientia.core.block;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges vanilla Bukkit block/item events to the Sapientia events and hooks
 * defined in {@code sapientia-api}. See ADR-012 and docs/api-spec.md §2.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>On {@link BlockPlaceEvent}: if the placed item carries a Sapientia PDC id
 *   and resolves to a registered {@link SapientiaBlock}, fire
 *   {@link SapientiaBlockPlaceEvent}, persist the placement, update the chunk
 *   index and invoke {@link SapientiaBlock#onPlace}.</li>
 *   <li>On {@link BlockBreakEvent}: if the block at that location is a Sapientia
 *   block, fire {@link SapientiaBlockBreakEvent}, invoke the block hook, remove
 *   persistence and drop the item form (unless suppressed by handlers).</li>
 *   <li>On {@link PlayerInteractEvent}: dispatch
 *   {@link SapientiaBlockInteractEvent} for right-clicks on Sapientia blocks and
 *   {@link SapientiaItemInteractEvent} for right-clicks using Sapientia items.</li>
 * </ul>
 */
public final class BlockLifecycleListener implements Listener {

    private final ItemRegistry itemRegistry;
    private final SapientiaBlockRegistry blockRegistry;
    private final ChunkBlockIndex index;
    private final CustomBlockStore store;

    public BlockLifecycleListener(
            @NotNull ItemRegistry itemRegistry,
            @NotNull SapientiaBlockRegistry blockRegistry,
            @NotNull ChunkBlockIndex index,
            @NotNull CustomBlockStore store) {
        this.itemRegistry = itemRegistry;
        this.blockRegistry = blockRegistry;
        this.index = index;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        ItemStack placed = event.getItemInHand();
        String pdcId = itemRegistry.idOf(placed);
        if (pdcId == null) {
            return;
        }
        NamespacedKey itemId = NamespacedKey.fromString(pdcId);
        if (itemId == null) {
            return;
        }
        SapientiaBlock def = blockRegistry.findByItemId(itemId).orElse(null);
        if (def == null) {
            return;
        }
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        SapientiaBlockPlaceEvent placeEvent = new SapientiaBlockPlaceEvent(
                player, block, def, placed.clone());
        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        BlockKey key = new BlockKey(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
        store.put(key, def.id().toString(), null);
        index.put(key, def);
        def.onPlace(placeEvent);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        SapientiaBlock def = index.at(block);
        if (def == null) {
            return;
        }

        SapientiaBlockBreakEvent breakEvent = new SapientiaBlockBreakEvent(
                event.getPlayer(), block, def);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        def.onBreak(breakEvent);

        BlockKey key = new BlockKey(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
        store.remove(key);
        index.remove(key);

        // Replace vanilla drops with the item form.
        event.setDropItems(false);
        if (breakEvent.dropItem()) {
            ItemStack drop = itemRegistry.createStack(def.itemId().toString(), 1);
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        // PlayerInteractEvent fires once per hand. We only react to the main-hand
        // pass to avoid double-firing onUse / opening the same UI twice.
        //
        // NOTE: We deliberately do NOT set ignoreCancelled=true: Paper frequently
        // delivers RIGHT_CLICK_AIR pre-cancelled when the item has no vanilla
        // action (e.g. KNOWLEDGE_BOOK without recipes), which would otherwise
        // silence our SapientiaItem#onUse for air-clicks. We still respect
        // cancellation when the click targets a real block.
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        Block clicked = event.getClickedBlock();
        if (clicked != null
                && (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK)) {
            // Respect pre-cancellation for block clicks (permission/region plugins).
            if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
                return;
            }
            SapientiaBlock def = index.at(clicked);
            if (def != null && action == Action.RIGHT_CLICK_BLOCK) {
                SapientiaBlockInteractEvent interactEvent = new SapientiaBlockInteractEvent(
                        event.getPlayer(), clicked, def, action, hand);
                Bukkit.getPluginManager().callEvent(interactEvent);
                // Always prevent vanilla interaction so Sapientia blocks never open
                // chests/workbenches unintentionally. Handlers can still no-op.
                event.setCancelled(true);
                if (!interactEvent.isCancelled()) {
                    def.onInteract(interactEvent);
                }
                return;
            }
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack used = event.getItem();
            if (used == null || used.getType() == Material.AIR) {
                return;
            }
            SapientiaItem item = itemRegistry.resolve(used);
            if (item == null) {
                return;
            }
            // Suppress vanilla side-effects (e.g. opening WRITTEN_BOOK UI for the guide,
            // eating food, etc.) BEFORE handlers run so any UI we open is not shadowed.
            event.setCancelled(true);
            SapientiaItemInteractEvent itemEvent = new SapientiaItemInteractEvent(
                    event.getPlayer(), used, item, action, hand);
            Bukkit.getPluginManager().callEvent(itemEvent);
            if (itemEvent.isCancelled()) {
                return;
            }
            item.onUse(itemEvent);
        }
    }
}
