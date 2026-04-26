package dev.brmz.sapientia.bedrock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.bedrock.forms.SapientiaSimpleForm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-generates a {@link SapientiaSimpleForm} from a {@link JavaInventoryRenderer}
 * for Bedrock players when the descriptor doesn't ship a dedicated Bedrock
 * renderer (T-206).
 *
 * <p>Algorithm: ask the renderer to populate an off-screen inventory, then
 * walk the slots; every non-empty, non-decorative stack becomes a button. Click
 * routes back to {@link JavaInventoryRenderer#onClick(Player, Object, int)}
 * with the original slot number.
 */
final class BedrockFallbackForm {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Logger logger;

    BedrockFallbackForm(@NotNull Logger logger) {
        this.logger = logger;
    }

    <C> void openFromJava(@NotNull Player player,
                          @NotNull UIDescriptor<C> descriptor,
                          @NotNull JavaInventoryRenderer<C> renderer,
                          @NotNull C context) {
        int size = renderer.size(player, context);
        if (size <= 0 || size % 9 != 0) {
            logger.warning(() -> "Skipping Bedrock fallback for " + descriptor.key()
                    + ": invalid Java inventory size " + size);
            return;
        }
        Component title = renderer.title(player, context);
        Inventory inv = Bukkit.createInventory(new OffscreenHolder(), size, title);
        try {
            renderer.render(inv, player, context);
        } catch (RuntimeException e) {
            logger.warning("Bedrock fallback render failed for " + descriptor.key() + ": " + e);
            return;
        }

        SapientiaSimpleForm form = new SapientiaSimpleForm()
                .title(LEGACY.serialize(title))
                .content("");

        List<Integer> slotMap = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (isDecorative(stack)) continue;
            String label = renderLabel(stack, slot);
            form.button(label);
            slotMap.add(slot);
        }
        if (slotMap.isEmpty()) {
            logger.warning(() -> "Bedrock fallback for " + descriptor.key()
                    + " had no clickable slots; aborting send.");
            return;
        }

        form.onClick(buttonIndex -> {
            if (buttonIndex < 0 || buttonIndex >= slotMap.size()) return;
            int slot = slotMap.get(buttonIndex);
            try {
                renderer.onClick(player, context, slot);
            } catch (RuntimeException e) {
                logger.warning("Bedrock fallback click handler failed for "
                        + descriptor.key() + " slot " + slot + ": " + e);
            }
        });
        form.onClose(() -> {
            try {
                renderer.onClose(player, context);
            } catch (RuntimeException e) {
                logger.warning("Bedrock fallback close handler failed for "
                        + descriptor.key() + ": " + e);
            }
        });
        form.send(player);
    }

    private static boolean isDecorative(@NotNull ItemStack stack) {
        Material m = stack.getType();
        // Glass panes are used uniformly as decorative borders in Sapientia UIs;
        // surfacing them as Bedrock buttons would clutter the form.
        return switch (m) {
            case BLACK_STAINED_GLASS_PANE,
                 WHITE_STAINED_GLASS_PANE,
                 GRAY_STAINED_GLASS_PANE,
                 LIGHT_GRAY_STAINED_GLASS_PANE,
                 BLUE_STAINED_GLASS_PANE,
                 RED_STAINED_GLASS_PANE,
                 GREEN_STAINED_GLASS_PANE,
                 YELLOW_STAINED_GLASS_PANE,
                 ORANGE_STAINED_GLASS_PANE -> true;
            default -> false;
        };
    }

    private static String renderLabel(@NotNull ItemStack stack, int slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) {
                return LEGACY.serialize(name);
            }
        }
        return stack.getType().name().toLowerCase().replace('_', ' ') + " (slot " + slot + ")";
    }

    /** Marker holder so the inventory we render into is recognisable as off-screen. */
    private static final class OffscreenHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() {
            throw new UnsupportedOperationException("off-screen fallback inventory");
        }
    }
}
