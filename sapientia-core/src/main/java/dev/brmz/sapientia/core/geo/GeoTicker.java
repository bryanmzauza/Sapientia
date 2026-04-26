package dev.brmz.sapientia.core.geo;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.api.multiblock.MultiblockShapeValidator;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import dev.brmz.sapientia.core.fluids.FluidServiceImpl;
import dev.brmz.sapientia.core.fluids.SimpleFluidNode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-tick kinetic loop driver for the 1.7.0 geo &amp; atmosphere catalogue
 * (T-431..T-436). Activates the placement-stub blocks that 1.7.0 shipped:
 *
 * <ul>
 *   <li>{@code quarry_controller} — drains energy when the 3×3×4 shell is
 *       intact, abstract "yields/tick" delivered as {@link
 *       BuiltinFluidTypes#WATER} slurry to the input tank above (proxy for
 *       1.8.0's mined-material output stream).</li>
 *   <li>{@code drill_rig_controller} — sub-bedrock virtual mining via a
 *       probability roll. With probability {@link #DRILL_HIT_PROB_PERMIL}
 *       per cycle, the rig produces {@link #DRILL_YIELD_MB} mB of
 *       {@link BuiltinFluidTypes#CRUDE_OIL} into the tank above (the
 *       deepest reservoirs are oil-rich).</li>
 *   <li>{@code desalinator_controller} — converts {@link
 *       BuiltinFluidTypes#WATER} above (proxy for "sea water" until the
 *       brine fluid lands in 2.0.0) into {@link BuiltinFluidTypes#WATER}
 *       below (proxy for "fresh water"). 100 mB → 90 mB, the missing
 *       10 mB models the rock-salt residue (item-form deferred to 2.0.0).</li>
 *   <li>{@code gas_extractor} — pulls {@link BuiltinFluidTypes#NITROGEN}
 *       out of the chunk atmosphere into the tank above.</li>
 *   <li>{@code atmospheric_collector} — round-robin through nitrogen, argon
 *       and carbon_dioxide so the player always sees output even without
 *       biome weighting (deferred to 2.0.0 alongside biome metadata).</li>
 * </ul>
 *
 * <p>The GPS coverage scan and handheld-map overlay (T-436 kinetic) require
 * a dedicated marker registry + persistence pass; those land in 1.8.0 with
 * the rest of advanced logistics. Quarry AABB persistence + wrench AABB
 * selector (T-440) are deferred to 1.8.0 for the same reason.
 */
public final class GeoTicker {

    /** Energy drawn per quarry cycle. */
    public static final long QUARRY_DRAW = 512L;
    /** mB of slurry pushed into the tank above the quarry per cycle. */
    public static final int  QUARRY_SLURRY_MB = 25;

    /** Energy drawn per drill-rig cycle (sub-bedrock virtual mining). */
    public static final long DRILL_RIG_DRAW = 1024L;
    /** Probability (per mille) that a drill-rig cycle produces a hit. */
    public static final int  DRILL_HIT_PROB_PERMIL = 200; // 20 %
    /** mB of crude_oil produced on a drill-rig hit. */
    public static final int  DRILL_YIELD_MB = 10;

    /** Energy drawn per desalinator cycle. */
    public static final long DESALINATOR_DRAW = 256L;
    /** Sea-water input drawn from the tank above. */
    public static final int  DESALINATOR_INPUT_MB  = 100;
    /** Fresh-water output offered to the tank below (90 % of input). */
    public static final int  DESALINATOR_OUTPUT_MB = 90;

    /** Energy drawn per gas-extractor cycle. */
    public static final long GAS_EXTRACTOR_DRAW = 256L;
    /** mB of nitrogen pulled from the chunk atmosphere per cycle. */
    public static final int  GAS_EXTRACTOR_MB   = 20;

    /** Energy drawn per atmospheric-collector cycle. */
    public static final long ATMO_COLLECTOR_DRAW = 256L;
    /** mB of atmospheric gas offered per cycle (round-robin). */
    public static final int  ATMO_COLLECTOR_MB   = 15;

    private final Logger logger;
    private final EnergyServiceImpl energy;
    private final FluidServiceImpl fluids;
    private final ChunkBlockIndex chunkIndex;

    private final NamespacedKey quarryId;
    private final NamespacedKey drillRigId;
    private final NamespacedKey desalinatorId;
    private final NamespacedKey gasExtractorId;
    private final NamespacedKey atmoCollectorId;

    /** Round-robin counter for the atmospheric collector (N₂ → Ar → CO₂ → N₂…). */
    private long atmoCounter;

    public GeoTicker(@NotNull Logger logger,
                     @NotNull org.bukkit.plugin.Plugin plugin,
                     @NotNull EnergyServiceImpl energy,
                     @NotNull FluidServiceImpl fluids,
                     @NotNull ChunkBlockIndex chunkIndex) {
        this.logger = logger;
        this.energy = energy;
        this.fluids = fluids;
        this.chunkIndex = chunkIndex;
        this.quarryId        = new NamespacedKey(plugin, "quarry_controller");
        this.drillRigId      = new NamespacedKey(plugin, "drill_rig_controller");
        this.desalinatorId   = new NamespacedKey(plugin, "desalinator_controller");
        this.gasExtractorId  = new NamespacedKey(plugin, "gas_extractor");
        this.atmoCollectorId = new NamespacedKey(plugin, "atmospheric_collector");
    }

    public void tick() {
        for (SimpleEnergyNode node : energy.graph().nodes()) {
            try {
                tickNode(node);
            } catch (RuntimeException ex) {
                logger.warning("GeoTicker error at " + node.location() + ": " + ex);
            }
        }
        atmoCounter++;
    }

    private void tickNode(SimpleEnergyNode node) {
        BlockKey key = node.location();
        World world = Bukkit.getWorld(key.world());
        if (world == null) return;
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        SapientiaBlock def = chunkIndex.at(block);
        if (def == null) return;
        NamespacedKey id = def.id();

        if (id.equals(quarryId)) {
            tickQuarry(node, block);
        } else if (id.equals(drillRigId)) {
            tickDrillRig(node, block);
        } else if (id.equals(desalinatorId)) {
            tickDesalinator(node, block);
        } else if (id.equals(gasExtractorId)) {
            tickGasExtractor(node, block);
        } else if (id.equals(atmoCollectorId)) {
            tickAtmoCollector(node, block);
        }
    }

    private void tickQuarry(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < QUARRY_DRAW) return;
        // Cheap structural gate — the 3×3×4 hollow shell of stainless casing
        // (or iron blocks as vanilla proxy) must still be intact.
        if (!MultiblockShapeValidator.validateHollowBox(block, 3, 3, 4,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK)) {
            return;
        }
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return;
        if (capacityFreeFor(tank, BuiltinFluidTypes.WATER) < QUARRY_SLURRY_MB) return;
        long inserted = tank.offer(BuiltinFluidTypes.WATER, QUARRY_SLURRY_MB);
        if (inserted <= 0L) return;
        node.draw(QUARRY_DRAW);
    }

    private void tickDrillRig(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < DRILL_RIG_DRAW) return;
        if (!MultiblockShapeValidator.validateHollowBox(block, 5, 5, 8,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK)) {
            return;
        }
        // Energy is always drawn (the rig is running). The yield is probabilistic.
        node.draw(DRILL_RIG_DRAW);
        int roll = ThreadLocalRandom.current().nextInt(1000);
        if (roll >= DRILL_HIT_PROB_PERMIL) return;
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return;
        if (capacityFreeFor(tank, BuiltinFluidTypes.CRUDE_OIL) < DRILL_YIELD_MB) return;
        tank.offer(BuiltinFluidTypes.CRUDE_OIL, DRILL_YIELD_MB);
    }

    private void tickDesalinator(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < DESALINATOR_DRAW) return;
        if (!MultiblockShapeValidator.validateHollowBox(block, 5, 3, 3,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.IRON_BLOCK)) {
            return;
        }
        SimpleFluidNode input  = tankAbove(block);
        SimpleFluidNode output = tankBelow(block);
        if (input == null || output == null) return;
        FluidType heldType = tankFuel(input);
        if (heldType == null || !heldType.id().equals(BuiltinFluidTypes.WATER.id())) return;
        if (input.contents() == null || input.contents().amountMb() < DESALINATOR_INPUT_MB) return;
        if (capacityFreeFor(output, BuiltinFluidTypes.WATER) < DESALINATOR_OUTPUT_MB) return;
        long drawn = input.draw(DESALINATOR_INPUT_MB);
        if (drawn < DESALINATOR_INPUT_MB) return;
        output.offer(BuiltinFluidTypes.WATER, DESALINATOR_OUTPUT_MB);
        node.draw(DESALINATOR_DRAW);
    }

    private void tickGasExtractor(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < GAS_EXTRACTOR_DRAW) return;
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return;
        if (capacityFreeFor(tank, BuiltinFluidTypes.NITROGEN) < GAS_EXTRACTOR_MB) return;
        long inserted = tank.offer(BuiltinFluidTypes.NITROGEN, GAS_EXTRACTOR_MB);
        if (inserted <= 0L) return;
        node.draw(GAS_EXTRACTOR_DRAW);
    }

    private void tickAtmoCollector(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < ATMO_COLLECTOR_DRAW) return;
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return;
        FluidType pick = atmoPick(atmoCounter);
        if (capacityFreeFor(tank, pick) < ATMO_COLLECTOR_MB) return;
        long inserted = tank.offer(pick, ATMO_COLLECTOR_MB);
        if (inserted <= 0L) return;
        node.draw(ATMO_COLLECTOR_DRAW);
    }

    /**
     * Round-robins through nitrogen, argon, carbon_dioxide. Public + package-private
     * so the catalogue test can lock the rotation contract without instantiating
     * the plugin.
     */
    static FluidType atmoPick(long counter) {
        long mod = Math.floorMod(counter, 3L);
        if (mod == 0L) return BuiltinFluidTypes.NITROGEN;
        if (mod == 1L) return BuiltinFluidTypes.ARGON;
        return BuiltinFluidTypes.CARBON_DIOXIDE;
    }

    private static @Nullable FluidType tankFuel(SimpleFluidNode tank) {
        return tank.contents() == null ? null : tank.contents().type();
    }

    private static long capacityFreeFor(SimpleFluidNode tank, FluidType desired) {
        long room = tank.capacityMb() - (tank.contents() == null ? 0L : tank.contents().amountMb());
        if (tank.contents() != null && !tank.contents().type().id().equals(desired.id())) return 0L;
        return room;
    }

    private SimpleFluidNode tankAbove(Block block) { return tankAt(block.getRelative(0, 1, 0)); }
    private SimpleFluidNode tankBelow(Block block) { return tankAt(block.getRelative(0, -1, 0)); }

    private SimpleFluidNode tankAt(Block block) {
        FluidNode node = fluids.nodeAt(block).orElse(null);
        return node instanceof SimpleFluidNode sn ? sn : null;
    }
}
