package dev.brmz.sapientia.core.mining;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps {@link PlacedBlockTracker} up to date: blocks placed by players or
 * endermen, formed by lava and water, or moved by pistons stop being natural.
 * Sand and gravel keep their origin while falling.
 */
public final class TerrainListener implements Listener {

    private final PlacedBlockTracker tracker;
    private final NamespacedKey fallingOrigin;

    public TerrainListener(@NotNull PlacedBlockTracker tracker, @NotNull String namespace) {
        this.tracker = tracker;
        this.fallingOrigin = new NamespacedKey(namespace, "placed_origin");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                tracker.markPlaced(state.getBlock());
            }
        } else {
            tracker.markPlaced(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(@NotNull BlockFormEvent event) {
        tracker.markPlaced(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(@NotNull BlockPistonExtendEvent event) {
        BlockFace facing = facing(event.getBlock(), event.getDirection());
        for (Block moved : event.getBlocks()) {
            tracker.markPlaced(moved.getRelative(facing));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(@NotNull BlockPistonRetractEvent event) {
        BlockFace towardPiston = facing(event.getBlock(), event.getDirection()).getOppositeFace();
        for (Block moved : event.getBlocks()) {
            tracker.markPlaced(moved.getRelative(towardPiston));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock falling) {
            if (event.getTo() == Material.AIR) {
                // Starts falling: remember whether it came from natural terrain.
                byte placed = (byte) (tracker.isNatural(event.getBlock()) ? 0 : 1);
                falling.getPersistentDataContainer().set(fallingOrigin, PersistentDataType.BYTE, placed);
            } else {
                Byte placed = falling.getPersistentDataContainer().get(fallingOrigin, PersistentDataType.BYTE);
                if (placed != null && placed == 0) {
                    tracker.markNatural(event.getBlock());
                } else {
                    tracker.markPlaced(event.getBlock());
                }
            }
            return;
        }
        if (event.getTo() != Material.AIR) {
            tracker.markPlaced(event.getBlock()); // endermen and other mobs placing blocks
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            tracker.onChunkGenerated(event.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        tracker.onChunkUnloaded(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        tracker.onWorldUnloaded(event.getWorld());
    }

    private static BlockFace facing(Block piston, BlockFace fallback) {
        return piston.getBlockData() instanceof Directional directional ? directional.getFacing() : fallback;
    }
}
