package dev.brmz.sapientia.core.android;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Per-archetype kinetic behaviour for the 8 androids (T-451 / T-454 / T-455
 * / 1.9.1).
 *
 * <p>The engine is invoked by {@link AndroidTicker} once per game tick, but
 * only after the per-android motor cooldown has elapsed
 * ({@link AndroidUpgradeScaling#motorCooldownTicks(int)}). The cooldown
 * gate is enforced by the ticker against {@code SimpleAndroidNode#lastTickMs}
 * which is repurposed in 1.9.1 to store "next eligible tick" rather than
 * "wall-clock millis of last tick". The persisted value still survives a
 * restart but is reset to {@code 0} on hydrate, so the rename is
 * backwards-compatible.
 *
 * <p>Each archetype follows the same skeleton:
 * <ol>
 *   <li>If the fuel buffer is empty, attempt to pull one fuel item from the
 *       chest above ({@link AndroidIO#pullFuelInto}). If nothing was
 *       absorbed, the android is idle this tick.</li>
 *   <li>Drain {@link AndroidUpgradeScaling#BIOFUEL_PER_INSTRUCTION} mb from
 *       the buffer (T-451 contract: at most one instruction per tick).</li>
 *   <li>Dispatch by {@link AndroidType}:
 *     <ul>
 *       <li>FARMER / LUMBERJACK / MINER / FISHERMAN / BUTCHER / SLAYER —
 *           roll {@link AndroidLootTables#roll} and deposit into the chest
 *           below.</li>
 *       <li>BUILDER — pull one block from the chest above and place it
 *           into a free air slot in front of the android.</li>
 *       <li>TRADER — pull 9 of any single material from the chest above and
 *           deposit 1 emerald into the chest below.</li>
 *     </ul>
 *   </li>
 * </ol>
 */
public final class AndroidBehaviorEngine {

    private final Logger logger;
    private final Map<AndroidType, Behaviour> behaviours = new EnumMap<>(AndroidType.class);

    public AndroidBehaviorEngine(@NotNull Logger logger) {
        this.logger = logger;
        behaviours.put(AndroidType.FARMER,     this::lootBehaviour);
        behaviours.put(AndroidType.LUMBERJACK, this::lootBehaviour);
        behaviours.put(AndroidType.MINER,      this::lootBehaviour);
        behaviours.put(AndroidType.FISHERMAN,  this::lootBehaviour);
        behaviours.put(AndroidType.BUTCHER,    this::lootBehaviour);
        behaviours.put(AndroidType.SLAYER,     this::lootBehaviour);
        behaviours.put(AndroidType.BUILDER,    this::builderBehaviour);
        behaviours.put(AndroidType.TRADER,     this::traderBehaviour);
    }

    @FunctionalInterface
    private interface Behaviour {
        boolean run(@NotNull SimpleAndroidNode node, long tickCount);
    }

    /**
     * Runs one instruction for the given node. Returns {@code true} when
     * work was actually performed (fuel was drained + an action succeeded);
     * {@code false} when the android idled (no fuel, no inputs, full output).
     */
    public boolean run(@NotNull SimpleAndroidNode node, long tickCount) {
        Behaviour b = behaviours.get(node.type());
        if (b == null) return false;
        try {
            return b.run(node, tickCount);
        } catch (Throwable t) {
            // Mirror the LogicService runOnce contract: faults must never
            // leak out of the per-tick loop.
            logger.warning("[AndroidBehaviorEngine] " + node.type() + " @ "
                    + node.block().getLocation() + " threw: " + t);
            return false;
        }
    }

    // ---- archetype implementations ----------------------------------------

    private boolean lootBehaviour(@NotNull SimpleAndroidNode node, long tickCount) {
        if (!ensureFuel(node)) return false;
        Inventory output = AndroidIO.outputBelow(node.block());
        if (output == null) return false;
        long seed = blockSeed(node.block()) ^ tickCount;
        AndroidLootTables.LootDrop drop = AndroidLootTables.roll(node.type(), seed);
        if (drop == null) return false;
        ItemStack stack = new ItemStack(drop.material(), drop.amount());
        if (!AndroidIO.depositOrReject(output, stack)) return false;
        consumeFuel(node);
        return true;
    }

    private boolean builderBehaviour(@NotNull SimpleAndroidNode node, long tickCount) {
        if (!ensureFuel(node)) return false;
        Inventory input = AndroidIO.inputAbove(node.block());
        if (input == null) return false;
        // Find a placeable block in the input chest.
        ItemStack source = null;
        int slot = -1;
        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack == null || stack.getAmount() <= 0) continue;
            if (!stack.getType().isBlock()) continue;
            source = stack;
            slot = i;
            break;
        }
        if (source == null) return false;
        // Place at an air slot in the chip-tier scan radius around the android.
        int radius = AndroidUpgradeScaling.chipScanRadius(node.chipTier());
        Block target = findAirInRadius(node.block(), radius);
        if (target == null) return false;
        target.setType(source.getType());
        source.setAmount(source.getAmount() - 1);
        if (source.getAmount() <= 0) input.setItem(slot, null);
        consumeFuel(node);
        return true;
    }

    private boolean traderBehaviour(@NotNull SimpleAndroidNode node, long tickCount) {
        if (!ensureFuel(node)) return false;
        Inventory input = AndroidIO.inputAbove(node.block());
        Inventory output = AndroidIO.outputBelow(node.block());
        if (input == null || output == null) return false;
        // Fixed 9-for-1 emerald exchange, mirroring vanilla villager pricing.
        ItemStack consumed = AndroidIO.consumeAny(input, 9);
        if (consumed == null) return false;
        ItemStack emerald = new ItemStack(Material.EMERALD, 1);
        if (!AndroidIO.depositOrReject(output, emerald)) {
            // Roll back: put items back in input.
            input.addItem(consumed);
            return false;
        }
        consumeFuel(node);
        return true;
    }

    // ---- helpers ----------------------------------------------------------

    private boolean ensureFuel(@NotNull SimpleAndroidNode node) {
        if (node.fuelBuffer() >= AndroidUpgradeScaling.BIOFUEL_PER_INSTRUCTION) return true;
        Inventory input = AndroidIO.inputAbove(node.block());
        if (input == null) return false;
        AndroidIO.pullFuelInto(node, input);
        return node.fuelBuffer() >= AndroidUpgradeScaling.BIOFUEL_PER_INSTRUCTION;
    }

    private void consumeFuel(@NotNull SimpleAndroidNode node) {
        node.setFuelBuffer(node.fuelBuffer() - AndroidUpgradeScaling.BIOFUEL_PER_INSTRUCTION);
    }

    private static long blockSeed(@NotNull Block b) {
        long h = 1469598103934665603L; // FNV offset basis, gives wide spread per coord.
        h ^= b.getX();          h *= 1099511628211L;
        h ^= b.getY();          h *= 1099511628211L;
        h ^= b.getZ();          h *= 1099511628211L;
        h ^= b.getWorld().getName().hashCode();
        return h;
    }

    private static Block findAirInRadius(@NotNull Block centre, int radius) {
        // Scan a small box around the android (skipping the android itself).
        // Limited to ±1 on Y so builders behave like a vanilla dispenser.
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block candidate = centre.getRelative(dx, dy, dz);
                    if (candidate.getType() == Material.AIR) return candidate;
                }
            }
        }
        return null;
    }
}
