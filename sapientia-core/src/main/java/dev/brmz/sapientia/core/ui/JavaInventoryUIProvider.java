package dev.brmz.sapientia.core.ui;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.ui.JavaInventoryRenderer;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.api.ui.UIProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/** Java Edition {@link UIProvider}. Always registered. See docs/ui-strategy.md §2. */
public final class JavaInventoryUIProvider implements UIProvider {

    private final UIService service;

    public JavaInventoryUIProvider(@NotNull UIService service) {
        this.service = service;
    }

    @Override
    public @NotNull PlatformType targetPlatform() {
        return PlatformType.JAVA;
    }

    @Override
    public <C> void openCustom(@NotNull Player player,
                                @NotNull UIDescriptor<C> descriptor,
                                @NotNull C context) {
        JavaInventoryRenderer<C> renderer = descriptor.javaRenderer();
        SapientiaInventoryHolder<C> holder = new SapientiaInventoryHolder<>(renderer, context);
        Inventory inventory = Bukkit.createInventory(
                holder,
                renderer.size(player, context),
                renderer.title(player, context));
        holder.bind(inventory);
        renderer.render(inventory, player, context);
        service.trackOpen(player, holder);
        player.openInventory(inventory);
    }
}
