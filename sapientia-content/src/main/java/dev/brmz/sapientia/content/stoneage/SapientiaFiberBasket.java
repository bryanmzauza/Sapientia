package dev.brmz.sapientia.content.stoneage;

import java.util.Arrays;
import java.util.Objects;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.events.SapientiaBlockBreakEvent;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The fibre basket (era 1): a 9-slot container that keeps its contents when
 * broken, like a shulker box. It stands on a vanilla dropper (which never
 * dispenses), so opening it and hoppers work as vanilla on Java and Bedrock.
 */
public final class SapientiaFiberBasket implements CatalogBlock {

    private final NamespacedKey id;
    private final NamespacedKey contentsKey;

    public SapientiaFiberBasket(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "fiber_basket");
        this.contentsKey = new NamespacedKey(plugin, "basket_contents");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.DROPPER; }
    @Override public @NotNull String displayNameKey() { return "block.fiber_basket.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.LOGISTICS; }
    @Override public boolean vanillaInteraction() { return true; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        StoneAgeBlocks.nameContainer(event);
        byte[] saved = event.placedStack().getItemMeta().getPersistentDataContainer()
                .get(contentsKey, PersistentDataType.BYTE_ARRAY);
        Inventory inventory = inventoryOf(event.block());
        if (saved != null && inventory != null) {
            inventory.setContents(ItemStack.deserializeItemsFromBytes(saved));
        }
    }

    @Override
    public void onBreak(@NotNull SapientiaBlockBreakEvent event) {
        Inventory inventory = inventoryOf(event.block());
        if (inventory == null || Arrays.stream(inventory.getContents()).allMatch(Objects::isNull)) return;
        ItemStack basket = Sapientia.get().createStack(id, 1).orElse(null);
        if (basket == null) return;
        byte[] contents = ItemStack.serializeItemsAsBytes(inventory.getContents());
        basket.editMeta(meta -> meta.getPersistentDataContainer().set(contentsKey, PersistentDataType.BYTE_ARRAY,
                contents));
        inventory.clear(); // the contents travel inside the basket instead of spilling
        event.dropItem(false);
        Block block = event.block();
        block.getWorld().dropItemNaturally(block.getLocation(), basket);
    }

    private static Inventory inventoryOf(Block block) {
        return block.getState(false) instanceof InventoryHolder holder ? holder.getInventory() : null;
    }
}
