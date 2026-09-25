package dev.brmz.sapientia.core.mining;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Drops mineral fragments when a player breaks natural rock. Only player
 * breaks count: explosions, pistons and fluids never yield minerals.
 */
public final class MiningListener implements Listener {

    private final MiningServiceImpl mining;

    public MiningListener(@NotNull MiningServiceImpl mining) {
        this.mining = mining;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!mining.isHost(block.getType())) return; // cheap check first: most breaks are not hosts
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || !event.isDropItems()) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack fragment : mining.rollFragments(block, tool)) {
            block.getWorld().dropItemNaturally(drop, fragment);
        }
    }
}
