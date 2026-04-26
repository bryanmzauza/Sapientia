package dev.brmz.sapientia.core.android;

import dev.brmz.sapientia.api.android.AndroidService;
import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.content.android.AndroidContentBlock;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels android placement events that would exceed the per-chunk
 * (T-456: 4) or server-wide (T-456: configurable, default 200) cap
 * before the block lands.
 *
 * <p>Runs at {@link EventPriority#HIGH}, after content-level listeners but
 * before {@link dev.brmz.sapientia.core.block.BlockLifecycleListener}
 * commits the block to persistence.
 */
public final class AndroidCapsListener implements Listener {

    private final AndroidService service;

    public AndroidCapsListener(@NotNull AndroidService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        if (!(event.definition() instanceof AndroidContentBlock)) {
            return;
        }
        Block block = event.block();
        String world = block.getWorld().getName();
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        if (service.countInChunk(world, chunkX, chunkZ) >= service.chunkCap()
                || service.totalCount() >= service.serverCap()) {
            event.setCancelled(true);
        }
    }
}
