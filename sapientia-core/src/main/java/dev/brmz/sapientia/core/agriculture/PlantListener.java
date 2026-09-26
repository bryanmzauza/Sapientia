package dev.brmz.sapientia.core.agriculture;

import java.util.concurrent.ThreadLocalRandom;

import dev.brmz.sapientia.api.agriculture.SapientiaPlant;
import dev.brmz.sapientia.api.agriculture.WildDrop;
import dev.brmz.sapientia.api.agriculture.WildSeedSource;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.progression.ProgressionService;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Planting, harvesting and wild seeds for Sapientia plants. Planting a
 * Sapientia seed places the vanilla crop and records it; breaking a recorded
 * crop drops the plant's own produce and seeds instead of the vanilla ones.
 * Era locks on planting are enforced by the era lock listener.
 */
public final class PlantListener implements Listener {

    private final PlantServiceImpl plants;
    private final ProgressionService progression;
    private final ItemRegistry items;

    public PlantListener(@NotNull PlantServiceImpl plants, @NotNull ProgressionService progression,
                         @NotNull ItemRegistry items) {
        this.plants = plants;
        this.progression = progression;
        this.items = items;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlant(@NotNull BlockPlaceEvent event) {
        String id = items.idOf(event.getItemInHand());
        if (id == null) return;
        NamespacedKey seed = NamespacedKey.fromString(id);
        if (seed == null) return;
        plants.bySeed(seed).ifPresent(plant -> {
            Block block = event.getBlockPlaced();
            if (block.getType() == plant.crop()) {
                plants.tracker().set(block, plant.id());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        SapientiaPlant plant = plants.plantAt(block).orElse(null);
        if (plant != null) {
            harvest(event, block, plant);
            return;
        }
        if (plants.tracker().plantAt(block) != null) {
            plants.tracker().clear(block); // stale record: the crop was replaced
        }
        if (plants.isWildHost(block.getType()) && player.getGameMode() != GameMode.CREATIVE) {
            wildSeeds(block, player);
        }
    }

    private void harvest(BlockBreakEvent event, Block block, SapientiaPlant plant) {
        plants.tracker().clear(block);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        event.setDropItems(false);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean mature = block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        if (mature) {
            drop(at, plant.produceItem(), random.nextInt(plant.produceMin(), plant.produceMax() + 1));
            drop(at, plant.seedItem(), random.nextInt(plant.seedsMin(), plant.seedsMax() + 1));
        } else {
            drop(at, plant.seedItem(), 1);
        }
    }

    /** Wild seeds and other wild drops (plant fibre); a hand tool that was needed loses one use. */
    private void wildSeeds(Block block, Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        String held = items.idOf(hand);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        NamespacedKey biome = block.getBiome().getKey();
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        boolean toolUsed = false;
        for (SapientiaPlant plant : plants.plants()) {
            if (!progression.isUnlocked(plant.era())) continue;
            for (WildSeedSource source : plant.wildSources()) {
                if (!applies(source, block, biome, held)) continue;
                toolUsed |= source.requiredTool() != null;
                if (random.nextDouble() < source.chance()) drop(at, plant.seedItem(), 1);
            }
        }
        for (WildDrop wild : plants.wildDrops()) {
            if (!progression.isUnlocked(wild.era()) || !applies(wild.source(), block, biome, held)) continue;
            toolUsed |= wild.source().requiredTool() != null;
            if (random.nextDouble() < wild.source().chance()) drop(at, wild.item(), 1);
        }
        SapientiaItem tool = toolUsed ? items.resolve(hand) : null;
        if (tool != null && tool.toolUses() > 0) {
            player.getInventory().setItemInMainHand(ItemRegistry.wear(player, hand));
        }
    }

    private static boolean applies(WildSeedSource source, Block block, NamespacedKey biome, String held) {
        return source.hosts().contains(block.getType())
                && (source.biomes().isEmpty() || source.biomes().contains(biome))
                && (source.requiredTool() == null || source.requiredTool().toString().equals(held));
    }

    private void drop(Location at, NamespacedKey item, int amount) {
        if (amount <= 0) return;
        ItemStack stack = items.createStack(item.toString(), amount);
        if (stack != null) at.getWorld().dropItemNaturally(at, stack);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        plants.tracker().onChunkUnloaded(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        plants.tracker().onWorldUnloaded(event.getWorld());
    }
}
