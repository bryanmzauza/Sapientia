package dev.brmz.sapientia.core.ui;

import java.util.List;

import dev.brmz.sapientia.api.ui.BedrockFormRenderer;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.bedrock.forms.SapientiaCustomForm;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.i18n.TextAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Experimental Item Filter UI (T-203 — 1.0.0). Gated behind the
 * {@code sapientia.experimental.filter} config flag because the filter logic
 * itself isn't wired into hoppers/pipes yet — this stub exists so
 * 1.0.0 ships the surface area cross-platform and a future minor can fill in
 * the matching engine without breaking compatibility.
 */
public final class FilterDescriptor implements UIDescriptor<Player> {

    public static final NamespacedKey KEY = NamespacedKey.fromString("sapientia:filter");

    private final Messages messages;

    public FilterDescriptor(@NotNull Messages messages) {
        this.messages = messages;
    }

    @Override
    public @NotNull NamespacedKey key() {
        return KEY;
    }

    @Override
    public @NotNull JavaInventoryRenderer<Player> javaRenderer() {
        return new FilterJavaRenderer();
    }

    @Override
    public @Nullable BedrockFormRenderer<Player> bedrockRenderer() {
        return new FilterBedrockRenderer();
    }

    /** Tiny 27-slot informational chest. Real filter logic ships in 1.x. */
    private final class FilterJavaRenderer implements JavaInventoryRenderer<Player> {
        @Override public int size(@NotNull Player p, @NotNull Player ctx) { return 27; }

        @Override public @NotNull Component title(@NotNull Player p, @NotNull Player ctx) {
            return messages.component("ui.filter.title");
        }

        @Override
        public void render(@NotNull Inventory inv, @NotNull Player p, @NotNull Player ctx) {
            ItemStack info = new ItemStack(Material.HOPPER);
            ItemMeta meta = info.getItemMeta();
            if (meta != null) {
                meta.displayName(messages.component("ui.filter.title"));
                meta.lore(List.of(messages.component("ui.filter.experimental")));
                info.setItemMeta(meta);
            }
            inv.setItem(13, info);
        }
    }

    private final class FilterBedrockRenderer implements BedrockFormRenderer<Player> {
        @Override
        public void open(@NotNull Player player, @NotNull Player ctx) {
            new SapientiaCustomForm()
                    .title(TextAdapter.toPlainBedrock(messages.component("ui.filter.title")))
                    .label(TextAdapter.toPlainBedrock(messages.component("ui.filter.experimental")))
                    .send(player);
        }
    }
}
