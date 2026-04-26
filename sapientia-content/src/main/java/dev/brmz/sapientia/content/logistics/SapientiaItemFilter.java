package dev.brmz.sapientia.content.logistics;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.logistics.ItemNode;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Demo LOW-tier item filter (T-300 / 1.1.0). Right-click opens the filter
 * configuration UI (Java inventory or Bedrock {@code CustomForm} via
 * {@code FilterDescriptor}).
 */
public final class SapientiaItemFilter extends LogisticsContentBlock {

    static final NamespacedKey FILTER_UI_KEY = NamespacedKey.fromString("sapientia:filter");

    public SapientiaItemFilter(@NotNull Plugin plugin) {
        super(plugin, "item_filter", Material.IRON_TRAPDOOR,
                "block.item_filter.name", ItemNodeType.FILTER, EnergyTier.LOW, 0);
    }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        ItemNode node = Sapientia.get().logistics().nodeAt(event.block()).orElse(null);
        if (node == null) {
            return;
        }
        if (FILTER_UI_KEY != null) {
            Sapientia.get().openUI(event.player(), FILTER_UI_KEY, node);
        }
    }
}
