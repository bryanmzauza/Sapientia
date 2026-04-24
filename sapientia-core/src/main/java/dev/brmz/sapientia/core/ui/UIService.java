package dev.brmz.sapientia.core.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.ui.UIDescriptor;
import dev.brmz.sapientia.api.ui.UIProvider;
import dev.brmz.sapientia.core.platform.PlatformService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Routes UI opens to the right provider based on the player's platform, and dispatches
 * inventory events back to the descriptor that created them. See docs/ui-strategy.md.
 */
public final class UIService implements Listener {

    private final PlatformService platformService;
    private final Map<PlatformType, UIProvider> providers = new EnumMap<>(PlatformType.class);
    private final Map<NamespacedKey, UIDescriptor<?>> descriptors = new HashMap<>();
    private final Map<UUID, SapientiaInventoryHolder<?>> openHolders = new HashMap<>();

    public UIService(@NotNull PlatformService platformService) {
        this.platformService = platformService;
    }

    public void registerProvider(@NotNull UIProvider provider) {
        providers.put(provider.targetPlatform(), provider);
    }

    public void register(@NotNull UIDescriptor<?> descriptor) {
        descriptors.put(descriptor.key(), descriptor);
    }

    public <C> void open(@NotNull Player player, @NotNull UIDescriptor<C> descriptor, @NotNull C context) {
        PlatformType platform = platformService.resolve(player);
        UIProvider provider = providers.getOrDefault(platform, providers.get(PlatformType.JAVA));
        if (provider == null) {
            throw new IllegalStateException("No UIProvider registered for any platform.");
        }
        provider.openCustom(player, descriptor, context);
    }

    /** Called by the Java provider to track the open inventory for event dispatch. */
    <C> void trackOpen(@NotNull Player player, @NotNull SapientiaInventoryHolder<C> holder) {
        openHolders.put(player.getUniqueId(), holder);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SapientiaInventoryHolder<?> sapientia)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            sapientia.dispatchClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SapientiaInventoryHolder<?>) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof SapientiaInventoryHolder<?> sapientia)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            openHolders.remove(player.getUniqueId());
            sapientia.dispatchClose(player);
        }
    }

    public void shutdown() {
        openHolders.clear();
    }
}
