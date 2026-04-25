package dev.brmz.sapientia.core.petroleum;

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

/**
 * Per-tick driver for the petroleum/biochemistry machine kinetic loop
 * (T-412 / T-413 / T-414 / T-415 / 1.5.1).
 *
 * <p>Block contracts (one production tick = one invocation of {@link #tick()},
 * scheduled at 5L from the plugin):
 * <ul>
 *   <li><b>pumpjack</b> — burns 256 SU, drains 50 mB from the chunk's
 *       {@link ReservoirService} and offers crude_oil to the fluid tank
 *       <em>directly above</em> the block.</li>
 *   <li><b>combustion_gen</b> — drains diesel (40 SU/mB) or gasoline (50 SU/mB)
 *       from the fluid tank below and tops up its own energy buffer.</li>
 *   <li><b>biogas_gen</b> — drains nutrient_broth (8 SU/mB) from the tank below
 *       into its energy buffer.</li>
 *   <li><b>oil_refinery_controller</b> — burns 1024 SU and converts 100 mB
 *       crude_oil from the input tank above into 40 mB diesel + 30 mB gasoline
 *       + 20 mB lubricant + 10 mB water (placeholder for tar) emitted to the
 *       four cardinal-adjacent tanks (N=diesel, E=gasoline, S=lubricant,
 *       W=tar/water). Requires a valid 5×5×7 stainless-steel hollow shell.</li>
 * </ul>
 *
 * <p>Fluid tank pickup uses {@link FluidServiceImpl#nodeAt(Block)} → first
 * tank-typed neighbour. If a required neighbour tank is missing the ticker
 * silently idles for that machine.
 */
public final class PetroleumTicker {

    private static final long PUMPJACK_DRAW = 256L;
    private static final int  PUMPJACK_MB   = 50;
    private static final long REFINERY_DRAW = 1024L;
    private static final int  REFINERY_BATCH_MB = 100;
    private static final int  REFINERY_DIESEL    = 40;
    private static final int  REFINERY_GASOLINE  = 30;
    private static final int  REFINERY_LUBRICANT = 20;
    private static final int  REFINERY_RESIDUE   = 10;
    /** SU per mB of liquid fuel. */
    private static final long DIESEL_SU_PER_MB   = 40L;
    private static final long GASOLINE_SU_PER_MB = 50L;
    private static final long BIOGAS_SU_PER_MB   = 8L;
    private static final int  COMBUSTION_DRAIN_MB = 5;
    private static final int  BIOGAS_DRAIN_MB     = 10;

    private final Logger logger;
    private final EnergyServiceImpl energy;
    private final FluidServiceImpl fluids;
    private final ChunkBlockIndex chunkIndex;
    private final ReservoirService reservoirs;

    private final NamespacedKey pumpjackId;
    private final NamespacedKey combustionId;
    private final NamespacedKey biogasId;
    private final NamespacedKey refineryId;

    public PetroleumTicker(@NotNull Logger logger,
                           @NotNull org.bukkit.plugin.Plugin plugin,
                           @NotNull EnergyServiceImpl energy,
                           @NotNull FluidServiceImpl fluids,
                           @NotNull ChunkBlockIndex chunkIndex,
                           @NotNull ReservoirService reservoirs) {
        this.logger = logger;
        this.energy = energy;
        this.fluids = fluids;
        this.chunkIndex = chunkIndex;
        this.reservoirs = reservoirs;
        this.pumpjackId   = new NamespacedKey(plugin, "pumpjack");
        this.combustionId = new NamespacedKey(plugin, "combustion_gen");
        this.biogasId     = new NamespacedKey(plugin, "biogas_gen");
        this.refineryId   = new NamespacedKey(plugin, "oil_refinery_controller");
    }

    public void tick() {
        for (SimpleEnergyNode node : energy.graph().nodes()) {
            try {
                tickNode(node);
            } catch (RuntimeException ex) {
                logger.warning("PetroleumTicker error at " + node.location() + ": " + ex);
            }
        }
    }

    private void tickNode(SimpleEnergyNode node) {
        BlockKey key = node.location();
        World world = Bukkit.getWorld(key.world());
        if (world == null) return;
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        SapientiaBlock def = chunkIndex.at(block);
        if (def == null) return;
        NamespacedKey id = def.id();

        if (id.equals(pumpjackId)) {
            tickPumpjack(node, block);
        } else if (id.equals(combustionId)) {
            tickCombustion(node, block);
        } else if (id.equals(biogasId)) {
            tickBiogas(node, block);
        } else if (id.equals(refineryId)) {
            tickRefinery(node, block);
        }
    }

    private void tickPumpjack(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < PUMPJACK_DRAW) return;
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return;
        int avail = reservoirs.amount(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        if (avail <= 0) return;
        int request = Math.min(PUMPJACK_MB, avail);
        long inserted = tank.offer(BuiltinFluidTypes.CRUDE_OIL, request);
        if (inserted <= 0L) return;
        reservoirs.drain(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ(), (int) inserted);
        node.draw(PUMPJACK_DRAW);
    }

    private void tickCombustion(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return;
        SimpleFluidNode tank = tankBelow(block);
        if (tank == null) return;
        FluidType fuel = tankFuel(tank);
        long suPerMb;
        if (fuel != null && fuel.id().equals(BuiltinFluidTypes.DIESEL.id())) {
            suPerMb = DIESEL_SU_PER_MB;
        } else if (fuel != null && fuel.id().equals(BuiltinFluidTypes.GASOLINE.id())) {
            suPerMb = GASOLINE_SU_PER_MB;
        } else {
            return;
        }
        long mbForRoom = (room + suPerMb - 1L) / suPerMb;
        long ask = Math.min((long) COMBUSTION_DRAIN_MB, mbForRoom);
        long burned = tank.draw(ask);
        if (burned <= 0L) return;
        long produced = Math.min(room, burned * suPerMb);
        node.offer(produced);
    }

    private void tickBiogas(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return;
        SimpleFluidNode tank = tankBelow(block);
        if (tank == null) return;
        FluidType fuel = tankFuel(tank);
        if (fuel == null || !fuel.id().equals(BuiltinFluidTypes.NUTRIENT_BROTH.id())) return;
        long mbForRoom = (room + BIOGAS_SU_PER_MB - 1L) / BIOGAS_SU_PER_MB;
        long ask = Math.min((long) BIOGAS_DRAIN_MB, mbForRoom);
        long burned = tank.draw(ask);
        if (burned <= 0L) return;
        node.offer(Math.min(room, burned * BIOGAS_SU_PER_MB));
    }

    private void tickRefinery(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < REFINERY_DRAW) return;
        // Cheap structural gate. Reuses the validator from the controller's
        // onPlace path so a partially-broken multiblock stops producing.
        if (!MultiblockShapeValidator.validateHollowBox(block, 5, 7, 5,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA)) {
            return;
        }
        SimpleFluidNode input = tankAbove(block);
        if (input == null) return;
        FluidType heldType = tankFuel(input);
        if (heldType == null || !heldType.id().equals(BuiltinFluidTypes.CRUDE_OIL.id())) return;
        SimpleFluidNode dieselOut    = tankAt(block.getRelative( 0, 0,  1));
        SimpleFluidNode gasolineOut  = tankAt(block.getRelative( 1, 0,  0));
        SimpleFluidNode lubricantOut = tankAt(block.getRelative( 0, 0, -1));
        SimpleFluidNode residueOut   = tankAt(block.getRelative(-1, 0,  0));
        if (dieselOut == null || gasolineOut == null || lubricantOut == null || residueOut == null) return;
        // Capacity preflight: don't drain crude unless every output can accept its share.
        if (input.contents() == null || input.contents().amountMb() < REFINERY_BATCH_MB) return;
        if (capacityFreeFor(dieselOut,    BuiltinFluidTypes.DIESEL)    < REFINERY_DIESEL)    return;
        if (capacityFreeFor(gasolineOut,  BuiltinFluidTypes.GASOLINE)  < REFINERY_GASOLINE)  return;
        if (capacityFreeFor(lubricantOut, BuiltinFluidTypes.LUBRICANT) < REFINERY_LUBRICANT) return;
        if (capacityFreeFor(residueOut,   BuiltinFluidTypes.WATER)     < REFINERY_RESIDUE)   return;
        long drawn = input.draw(REFINERY_BATCH_MB);
        if (drawn < REFINERY_BATCH_MB) {
            // Shouldn't happen given the preflight but be safe.
            return;
        }
        dieselOut.offer(BuiltinFluidTypes.DIESEL, REFINERY_DIESEL);
        gasolineOut.offer(BuiltinFluidTypes.GASOLINE, REFINERY_GASOLINE);
        lubricantOut.offer(BuiltinFluidTypes.LUBRICANT, REFINERY_LUBRICANT);
        residueOut.offer(BuiltinFluidTypes.WATER, REFINERY_RESIDUE);
        node.draw(REFINERY_DRAW);
    }

    private static FluidType tankFuel(SimpleFluidNode tank) {
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
