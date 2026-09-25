package dev.brmz.sapientia.core.engine;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.block.CustomBlockStore;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.fluids.FluidServiceImpl;
import dev.brmz.sapientia.core.fluids.SimpleFluidNode;
import dev.brmz.sapientia.core.logistics.ItemServiceImpl;
import dev.brmz.sapientia.core.persistence.DatabaseWorker;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Loads Sapientia state for chunks without touching the database on the main
 * thread. On chunk load, the blocks and the energy, item and fluid nodes are
 * read on the database thread; the main thread applies finished reads at the
 * start of each tick, within a time budget. A chunk unloaded or loaded again
 * before its read is applied discards the stale read.
 */
public final class ChunkHydrator implements Listener {

    private final ChunkBlockIndex index;
    private final CustomBlockStore blocks;
    private final EnergyServiceImpl energy;
    private final ItemServiceImpl items;
    private final FluidServiceImpl fluids;
    private final DatabaseWorker database;
    private final ConcurrentLinkedQueue<Loaded> ready = new ConcurrentLinkedQueue<>();

    private record Loaded(String world, int chunkX, int chunkZ, long token,
                          List<CustomBlockStore.StoredBlock> blocks,
                          List<SimpleEnergyNode> energy,
                          ItemServiceImpl.Loaded items,
                          List<SimpleFluidNode> fluids) {}

    public ChunkHydrator(@NotNull ChunkBlockIndex index, @NotNull CustomBlockStore blocks,
                         @NotNull EnergyServiceImpl energy, @NotNull ItemServiceImpl items,
                         @NotNull FluidServiceImpl fluids, @NotNull DatabaseWorker database) {
        this.index = index;
        this.blocks = blocks;
        this.energy = energy;
        this.items = items;
        this.fluids = fluids;
        this.database = database;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        request(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        unload(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /** Queues a chunk read on the database thread. */
    public void request(@NotNull String world, int chunkX, int chunkZ) {
        long token = index.beginLoad(world, chunkX, chunkZ);
        database.execute(() -> ready.add(read(world, chunkX, chunkZ, token)));
    }

    /** Reads and applies a chunk on the calling thread (plugin start-up). */
    public void loadNow(@NotNull String world, int chunkX, int chunkZ) {
        apply(read(world, chunkX, chunkZ, index.beginLoad(world, chunkX, chunkZ)));
    }

    /** Applies finished reads until the budget runs out; returns how many were applied. */
    public int drain(long budgetNanos) {
        long deadline = System.nanoTime() + budgetNanos;
        int applied = 0;
        Loaded loaded;
        while ((loaded = ready.poll()) != null) {
            apply(loaded);
            applied++;
            if (System.nanoTime() >= deadline) break;
        }
        return applied;
    }

    /** Reads waiting to be applied. */
    public int backlog() {
        return ready.size();
    }

    /** Drops a chunk's blocks and nodes; unsaved node state is queued for writing first. */
    public void unload(@NotNull String world, int chunkX, int chunkZ) {
        index.unloadChunk(world, chunkX, chunkZ);
        energy.unloadChunk(world, chunkX, chunkZ);
        items.unloadChunk(world, chunkX, chunkZ);
        fluids.unloadChunk(world, chunkX, chunkZ);
    }

    private Loaded read(String world, int chunkX, int chunkZ, long token) {
        return new Loaded(world, chunkX, chunkZ, token,
                blocks.loadChunk(world, chunkX, chunkZ),
                energy.store().loadChunk(world, chunkX, chunkZ),
                items.loadChunk(world, chunkX, chunkZ),
                fluids.loadChunk(world, chunkX, chunkZ));
    }

    private void apply(Loaded loaded) {
        if (!index.apply(loaded.world(), loaded.chunkX(), loaded.chunkZ(), loaded.token(), loaded.blocks())) {
            return; // unloaded or loaded again since the read was queued
        }
        energy.applyLoaded(loaded.energy());
        items.applyLoaded(loaded.items());
        fluids.applyLoaded(loaded.fluids());
    }
}
