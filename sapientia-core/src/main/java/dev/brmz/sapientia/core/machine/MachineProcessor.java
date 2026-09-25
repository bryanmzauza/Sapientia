package dev.brmz.sapientia.core.machine;

import java.util.HashMap;
import java.util.Map;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.machine.MachineRecipe;
import dev.brmz.sapientia.api.machine.MachineRecipeRegistry;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.engine.MachineBehavior;
import dev.brmz.sapientia.core.engine.MachineContext;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Recipe processing for machines that have {@link MachineRecipe}s.
 *
 * <p>I/O contract:
 * <ul>
 *   <li>The vanilla container <strong>directly above</strong> the machine is the
 *       input; the first stack that matches a recipe is used.</li>
 *   <li>The vanilla container <strong>directly below</strong> is the output; if it
 *       is full, the machine waits without consuming the input.</li>
 *   <li>Energy is drawn from the machine's own {@link EnergyNode#bufferCurrent()
 *       buffer} when a recipe completes.</li>
 * </ul>
 *
 * <p>Each machine costs one scheduler event per recipe: starting a recipe
 * schedules its completion {@code ticksRequired} machine ticks later, and
 * nothing runs in between. Without input the machine backs off; while waiting
 * for energy or output space it retries every {@value #RETRY_TICKS} ticks.
 * Progress lives in memory only: a restart or chunk unload restarts the recipe.
 */
public final class MachineProcessor {

    /** Game ticks per recipe "machine tick" ({@link MachineRecipe#ticksRequired()} unit). */
    public static final int MACHINE_TICK = 10;
    static final int RETRY_TICKS = 20;

    private final EnergyServiceImpl energy;
    private final MachineRecipeRegistry recipes;
    private final Map<BlockKey, InFlight> inFlight = new HashMap<>();

    public MachineProcessor(@NotNull EnergyServiceImpl energy, @NotNull MachineRecipeRegistry recipes) {
        this.energy = energy;
        this.recipes = recipes;
    }

    /** Registers the recipe behaviour for every block type that has machine recipes. */
    public void registerBehaviors(@NotNull SapientiaEngine engine, @NotNull Iterable<SapientiaBlock> blocks) {
        for (SapientiaBlock block : blocks) {
            if (!recipes.recipesFor(block.id()).isEmpty() && !engine.isProcessor(block.id())) {
                engine.registerBehavior(block.id(), RETRY_TICKS, context -> run(engine, block, context));
            }
        }
    }

    private int run(SapientiaEngine engine, SapientiaBlock definition, MachineContext context) {
        BlockKey key = engine.keyOf(context);
        SimpleEnergyNode node = energy.graph().nodeAt(key);
        Block block = engine.blockOf(context);
        if (node == null || block == null || node.type() != EnergyNodeType.CONSUMER) {
            inFlight.remove(key);
            return MachineBehavior.idle(RETRY_TICKS);
        }
        InFlight current = inFlight.get(key);
        if (current != null) {
            int result = complete(node, block, key, current);
            if (result != 0) {
                return result; // still waiting for energy or output space
            }
        }
        return start(node, block, key, definition);
    }

    /** Starts the next recipe; returns the delay until it completes, or an idle result. */
    private int start(SimpleEnergyNode node, Block block, BlockKey key, SapientiaBlock definition) {
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        if (input == null) {
            return MachineBehavior.idle(RETRY_TICKS);
        }
        for (int slot = 0; slot < input.getSize(); slot++) {
            ItemStack candidate = input.getItem(slot);
            if (candidate == null || candidate.getAmount() <= 0) continue;
            MachineRecipe recipe = recipes.findMatching(definition.id(), candidate);
            if (recipe == null) continue;
            if (node.bufferCurrent() < recipe.energyCost()) {
                return RETRY_TICKS;
            }
            inFlight.put(key, new InFlight(recipe, slot));
            return Math.max(1, recipe.ticksRequired() * MACHINE_TICK);
        }
        return MachineBehavior.idle(RETRY_TICKS);
    }

    /**
     * Finishes an in-flight recipe. Returns {@code 0} when it is done (or had to be
     * abandoned) so the next one can start, or a retry delay while blocked.
     */
    private int complete(SimpleEnergyNode node, Block block, BlockKey key, InFlight current) {
        MachineRecipe recipe = current.recipe();
        if (node.bufferCurrent() < recipe.energyCost()) {
            return RETRY_TICKS;
        }
        Inventory input = inventoryAt(block.getRelative(0, 1, 0));
        Inventory output = inventoryAt(block.getRelative(0, -1, 0));
        ItemStack slotStack = input == null ? null : input.getItem(current.inputSlot());
        if (output == null || slotStack == null || !recipe.matches(slotStack)) {
            inFlight.remove(key);
            return 0;
        }
        if (!output.addItem(recipe.output().clone()).isEmpty()) {
            return RETRY_TICKS; // output full; input untouched
        }
        slotStack.setAmount(slotStack.getAmount() - recipe.input().getAmount());
        if (slotStack.getAmount() <= 0) {
            input.setItem(current.inputSlot(), null);
        }
        node.draw(recipe.energyCost());
        inFlight.remove(key);
        return 0;
    }

    private static Inventory inventoryAt(Block block) {
        BlockState state = block.getState(false);
        return state instanceof InventoryHolder holder ? holder.getInventory() : null;
    }

    /** Called when a machine block is broken so any in-flight recipe is dropped. */
    public void onBlockBroken(@NotNull BlockKey key) {
        inFlight.remove(key);
    }

    public int inFlightCount() {
        return inFlight.size();
    }

    private record InFlight(MachineRecipe recipe, int inputSlot) {}
}
