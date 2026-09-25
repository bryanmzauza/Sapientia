package dev.brmz.sapientia.core.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.progression.ProgressionServiceImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the name and era of the Sapientia block or dropped item under each
 * player's crosshair on the action bar. Runs as an engine system task: one
 * short ray trace per online player per run, nothing while no player looks at
 * Sapientia content. Energy nodes looked at with the wrench are left to the
 * energy inspector.
 */
public final class LookAtHud implements Listener {

    private static final String WRENCH_ID = "sapientia:wrench";
    /** Returned (by identity) when the energy inspector owns the player's action bar. */
    private static final Component WRENCH = Component.text("wrench");

    private final ChunkBlockIndex index;
    private final ItemRegistry items;
    private final EnergyServiceImpl energy;
    private final ProgressionServiceImpl progression;
    private final Messages messages;
    private final double range;
    /** Players whose action bar shows a name now; cleared once they look away. */
    private final Set<UUID> showing = new HashSet<>();

    public LookAtHud(@NotNull ChunkBlockIndex index, @NotNull ItemRegistry items, @NotNull EnergyServiceImpl energy,
                     @NotNull ProgressionServiceImpl progression, @NotNull Messages messages, double range) {
        this.index = index;
        this.items = items;
        this.energy = energy;
        this.progression = progression;
        this.messages = messages;
        this.range = Math.max(1.0, range);
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Component text = player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING
                    ? describe(player) : null;
            if (text == WRENCH) {
                showing.remove(player.getUniqueId());
            } else if (text != null) {
                player.sendActionBar(text);
                showing.add(player.getUniqueId());
            } else if (showing.remove(player.getUniqueId())) {
                player.sendActionBar(Component.empty());
            }
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        showing.remove(event.getPlayer().getUniqueId());
    }

    private @Nullable Component describe(@NotNull Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTrace(eye, eye.getDirection(), range,
                FluidCollisionMode.NEVER, true, 0.2, entity -> entity instanceof Item);
        if (hit == null) return null;
        if (hit.getHitEntity() instanceof Item dropped) {
            ItemStack stack = dropped.getItemStack();
            NamespacedKey id = keyOf(items.idOf(stack));
            SapientiaItem item = id == null ? null : items.find(id).orElse(null);
            if (item == null) return null;
            return line("hud.look-at.item", item.displayNameKey(), id, stack.getAmount());
        }
        Block block = hit.getHitBlock();
        SapientiaBlock sapientia = block == null ? null : index.at(block);
        if (sapientia == null) return null;
        if (WRENCH_ID.equals(items.idOf(player.getInventory().getItemInMainHand()))
                && energy.nodeAt(block).isPresent()) {
            return WRENCH;
        }
        return line("hud.look-at.block", sapientia.displayNameKey(), sapientia.id(), 1);
    }

    private Component line(String key, String nameKey, NamespacedKey id, int amount) {
        Era era = progression.eraOf(id);
        Component text = messages.component(key,
                Placeholder.component("name", messages.component(nameKey)),
                Placeholder.unparsed("amount", Integer.toString(amount)),
                Placeholder.unparsed("number", Integer.toString(era.number())),
                Placeholder.component("era", messages.component(era.nameKey())));
        return progression.isUnlocked(era) ? text : text.append(messages.component("hud.look-at.locked"));
    }

    private static @Nullable NamespacedKey keyOf(@Nullable String id) {
        return id == null ? null : NamespacedKey.fromString(id);
    }
}
