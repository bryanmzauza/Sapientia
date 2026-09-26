package dev.brmz.sapientia.content.stoneage;

import java.util.Map;
import java.util.WeakHashMap;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The hand quern (era 1): right-clicking it with wheat grinds; every third
 * click turns one wheat into one flour. Manual only, no automation.
 */
public final class SapientiaHandQuern implements CatalogBlock {

    static final int CLICKS_PER_FLOUR = 3;

    private final NamespacedKey id;
    private final NamespacedKey flour;
    private final NamespacedKey itemIdKey = NamespacedKey.fromString("sapientia:item_id");
    /** Grinding clicks so far, per player (entries go away with the player object). */
    private final Map<Player, Integer> clicks = new WeakHashMap<>();

    public SapientiaHandQuern(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "hand_quern");
        this.flour = new NamespacedKey(plugin, "flour");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.GRINDSTONE; }
    @Override public @NotNull String displayNameKey() { return "block.hand_quern.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }
    @Override public int chunkLimit() { return 16; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Player player = event.player();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.WHEAT || hand.getItemMeta().getPersistentDataContainer().has(itemIdKey)) {
            clicks.remove(player);
            return; // only vanilla wheat; flax and other Sapientia items are not grain
        }
        int count = clicks.merge(player, 1, Integer::sum);
        player.playSound(event.block().getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.8f + 0.1f * count);
        if (count < CLICKS_PER_FLOUR) return;
        clicks.remove(player);
        hand.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        Sapientia.get().createStack(flour, 1).ifPresent(stack -> player.getInventory().addItem(stack).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow)));
        Sapientia.get().progression().discover(player.getUniqueId(), flour);
    }
}
