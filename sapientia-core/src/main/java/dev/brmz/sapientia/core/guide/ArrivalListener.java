package dev.brmz.sapientia.core.guide;

import java.util.List;

import dev.brmz.sapientia.api.progression.ProgressionService;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Era 0 (Arrival): on a player's first join they receive the guide and a short
 * explanation, and every join adds the Sapientia recipes of the vanilla
 * crafting table to their recipe book.
 */
public final class ArrivalListener implements Listener {

    private static final NamespacedKey GUIDE = new NamespacedKey("sapientia", "guide");

    private final ItemRegistry items;
    private final Messages messages;
    private final ProgressionService progression;
    private final List<NamespacedKey> vanillaRecipes;
    private final boolean giveGuide;
    private final NamespacedKey guideGiven;

    /** @param vanillaRecipes keys of the installed vanilla crafting table recipes */
    public ArrivalListener(@NotNull ItemRegistry items, @NotNull Messages messages,
                           @NotNull ProgressionService progression, @NotNull List<NamespacedKey> vanillaRecipes,
                           boolean giveGuide, @NotNull String namespace) {
        this.items = items;
        this.messages = messages;
        this.progression = progression;
        this.vanillaRecipes = List.copyOf(vanillaRecipes);
        this.giveGuide = giveGuide;
        this.guideGiven = new NamespacedKey(namespace, "guide_given");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.discoverRecipes(vanillaRecipes);
        if (!giveGuide || player.getPersistentDataContainer().has(guideGiven, PersistentDataType.BYTE)) return;
        player.getPersistentDataContainer().set(guideGiven, PersistentDataType.BYTE, (byte) 1);
        if (!progression.isAvailable(GUIDE)) return;
        ItemStack guide = items.createStack(GUIDE.toString(), 1);
        if (guide == null) return;
        player.getInventory().addItem(guide).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        player.sendMessage(messages.component("guide.welcome"));
    }
}
