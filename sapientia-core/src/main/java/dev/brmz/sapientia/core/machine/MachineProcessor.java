package dev.brmz.sapientia.core.machine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.machine.MachineRecipe;
import dev.brmz.sapientia.api.machine.MachineRecipeRegistry;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Per-tick recipe processor for {@link dev.brmz.sapientia.content.energy.MachineEnergyBlock}
 * blocks (T-404 / T-405 / T-414 / 1.4.1 / 1.5.1).
 *
 * <p>I/O contract (intentionally minimal):
 * <ul>
 *   <li>The vanilla container <strong>directly above</strong> the machine block
 *       (chest, barrel, dispenser, dropper, hopper, etc.) acts as the input
 *       buffer. The processor scans its slots for the first matching
 *       {@link MachineRecipe}.</li>
 *   <li>The vanilla container <strong>directly below</strong> the machine block
 *       acts as the output buffer. Output stacks are deposited via
 *       {@link Inventory#addItem(ItemStack...)}; if the output buffer is full
 *       the recipe rolls back without consuming the input.</li>
 *   <li>Energy is drained from the machine's own
 *       {@link EnergyNode#bufferCurrent() buffer} once per recipe completion.</li>
 * </ul>
 *
 * <p>Recipe progress lives in memory only: server restarts roll back any
 * in-flight recipe (input remains in the input chest, no output produced).
 */
public final class MachineProcessor {

    private final Logger logger;
    private final EnergyServiceImpl energy;
    private final MachineRecipeRegistry recipes;
    private final dev.brmz.sapientia.core.block.ChunkBlockIndex chunkIndex;

    private final Map<BlockKey, InFlight> inFlight = new ConcurrentHashMap<>();

    public MachineProcessor(@NotNull Logger logger,
                            @NotNull EnergyServiceImpl energy,
                            @NotNull MachineRecipeRegistry recipes,
                            @NotNull dev.brmz.sapientia.core.block.ChunkBlockIndex chunkIndex) {
        this.logger = logger;
        this.energy = energy;
        this.recipes = recipes;
        this.chunkIndex = chunkIndex;
    }

    /** Scheduler entry — call once per machine tick (10 ticks recommended). */
    public void tick() {
        // Reap stale in-flight entries whose energy node has been removed
        // (e.g. block broken). Cheap: bounded by inFlight size which is small.
        if (!inFlight.isEmpty()) {
            inFlight.keySet().removeIf(key -> energy.graph().nodeAt(key) == null);
        }
        for (SimpleEnergyNode node : energy.graph().nodes()) {
            if (node.type() != dev.brmz.sapientia.api.energy.EnergyNodeType.CONSUMER) continue;
            tickNode(node);
        }
    }

    private void tickNode(SimpleEnergyNode node) {
        BlockKey key = node.location();
        World world = Bukkit.getWorld(key.world());
        if (world == null) return;
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        SapientiaBlock def = chunkIndex.at(block);
        if (def == null) return;
        NamespacedKey machineId = def.id();
        if (recipes.recipesFor(machineId).isEmpty()) return;

        InFlight cur = inFlight.get(key);
        if (cur == null) {
            startRecipe(node, block, machineId);
        } else {
            advanceRecipe(node, block, key, cur);
        }
    }

    private void startRecipe(SimpleEnergyNode node, Block block, NamespacedKey machineId) {
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        if (input == null) return;
        for (int slot = 0; slot < input.getSize(); slot++) {
            ItemStack candidate = input.getItem(slot);
            if (candidate == null || candidate.getAmount() <= 0) continue;
            MachineRecipe recipe = recipes.findMatching(machineId, candidate);
            if (recipe == null) continue;
            if (node.bufferCurrent() < recipe.energyCost()) return;
            inFlight.put(node.location(),
                    new InFlight(recipe, slot, UUID.randomUUID().toString(), 0));
            return;
        }
    }

    private void advanceRecipe(SimpleEnergyNode node, Block block, BlockKey key, InFlight cur) {
        int next = cur.ticksElapsed + 1;
        if (next < cur.recipe.ticksRequired()) {
            inFlight.put(key, new InFlight(cur.recipe, cur.inputSlot, cur.runId, next));
            return;
        }
        if (node.bufferCurrent() < cur.recipe.energyCost()) {
            return; // Wait for energy.
        }
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        Inventory output = inventoryAt(block.getRelative(0, -1, 0));
        if (input == null || output == null) {
            inFlight.remove(key);
            return;
        }
        ItemStack slotStack = input.getItem(cur.inputSlot);
        if (!cur.recipe.matches(slotStack)) {
            inFlight.remove(key);
            return;
        }
        ItemStack toAdd = cur.recipe.output().clone();
        Map<Integer, ItemStack> rejected = output.addItem(toAdd);
        if (!rejected.isEmpty()) {
            return; // Output full; retry next tick.
        }
        slotStack.setAmount(slotStack.getAmount() - cur.recipe.input().getAmount());
        if (slotStack.getAmount() <= 0) input.setItem(cur.inputSlot, null);
        node.draw(cur.recipe.energyCost());
        inFlight.remove(key);
    }

    private static Inventory inventoryAt(Block block) {
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        return null;
    }

    /** Called when a machine block is broken so we abandon any in-flight recipe. */
    public void onBlockBroken(@NotNull BlockKey key) {
        inFlight.remove(key);
    }

    public int inFlightCount() { return inFlight.size(); }

    @SuppressWarnings("unused")
    private Logger logger() { return logger; }

    private record InFlight(MachineRecipe recipe, int inputSlot, String runId, int ticksElapsed) {}
}
