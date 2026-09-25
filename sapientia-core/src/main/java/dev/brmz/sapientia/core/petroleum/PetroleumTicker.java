package dev.brmz.sapientia.core.petroleum;


import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.api.multiblock.MultiblockShapeValidator;
import dev.brmz.sapientia.core.energy.EnergyMachineBehavior;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import dev.brmz.sapientia.core.fluids.FluidServiceImpl;
import dev.brmz.sapientia.core.fluids.SimpleFluidNode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    private final EnergyServiceImpl energy;
    private final FluidServiceImpl fluids;
    private final ReservoirService reservoirs;

    private final NamespacedKey pumpjackId;
    private final NamespacedKey combustionId;
    private final NamespacedKey biogasId;
    private final NamespacedKey refineryId;

    public PetroleumTicker(@NotNull org.bukkit.plugin.Plugin plugin,
                           @NotNull EnergyServiceImpl energy,
                           @NotNull FluidServiceImpl fluids,
                           @NotNull ReservoirService reservoirs) {
        this.energy = energy;
        this.fluids = fluids;
        this.reservoirs = reservoirs;
        this.pumpjackId   = new NamespacedKey(plugin, "pumpjack");
        this.combustionId = new NamespacedKey(plugin, "combustion_gen");
        this.biogasId     = new NamespacedKey(plugin, "biogas_gen");
        this.refineryId   = new NamespacedKey(plugin, "oil_refinery_controller");
    }

    /** Ticks between production steps while a machine keeps working. */
    public static final int PERIOD = 5;

    /** Registers the behaviour of each block type driven by this class. */
    public void registerBehaviors(@NotNull SapientiaEngine engine) {
        engine.registerBehavior(pumpjackId, PERIOD, EnergyMachineBehavior.of(engine, energy, PERIOD, this::tickPumpjack));
        engine.registerBehavior(combustionId, PERIOD, EnergyMachineBehavior.of(engine, energy, PERIOD, this::tickCombustion));
        engine.registerBehavior(biogasId, PERIOD, EnergyMachineBehavior.of(engine, energy, PERIOD, this::tickBiogas));
        engine.registerBehavior(refineryId, PERIOD, EnergyMachineBehavior.of(engine, energy, PERIOD, this::tickRefinery));
    }

    private boolean tickPumpjack(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return false;
        if (node.bufferCurrent() < PUMPJACK_DRAW) return false;
        SimpleFluidNode tank = tankAbove(block);
        if (tank == null) return false;
        int avail = reservoirs.amount(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        if (avail <= 0) return false;
        int request = Math.min(PUMPJACK_MB, avail);
        long inserted = tank.offer(BuiltinFluidTypes.CRUDE_OIL, request);
        if (inserted <= 0L) return false;
        reservoirs.drain(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ(), (int) inserted);
        node.draw(PUMPJACK_DRAW);
        return true;
    }

    private boolean tickCombustion(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return false;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return false;
        SimpleFluidNode tank = tankBelow(block);
        if (tank == null) return false;
        FluidType fuel = tankFuel(tank);
        long suPerMb;
        if (fuel != null && fuel.id().equals(BuiltinFluidTypes.DIESEL.id())) {
            suPerMb = DIESEL_SU_PER_MB;
        } else if (fuel != null && fuel.id().equals(BuiltinFluidTypes.GASOLINE.id())) {
            suPerMb = GASOLINE_SU_PER_MB;
        } else {
            return false;
        }
        long mbForRoom = (room + suPerMb - 1L) / suPerMb;
        long ask = Math.min((long) COMBUSTION_DRAIN_MB, mbForRoom);
        long burned = tank.draw(ask);
        if (burned <= 0L) return false;
        long produced = Math.min(room, burned * suPerMb);
        node.offer(produced);
        return true;
    }

    private boolean tickBiogas(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return false;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return false;
        SimpleFluidNode tank = tankBelow(block);
        if (tank == null) return false;
        FluidType fuel = tankFuel(tank);
        if (fuel == null || !fuel.id().equals(BuiltinFluidTypes.NUTRIENT_BROTH.id())) return false;
        long mbForRoom = (room + BIOGAS_SU_PER_MB - 1L) / BIOGAS_SU_PER_MB;
        long ask = Math.min((long) BIOGAS_DRAIN_MB, mbForRoom);
        long burned = tank.draw(ask);
        if (burned <= 0L) return false;
        node.offer(Math.min(room, burned * BIOGAS_SU_PER_MB));
        return true;
    }

    private boolean tickRefinery(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return false;
        if (node.bufferCurrent() < REFINERY_DRAW) return false;
        // Cheap structural gate. Reuses the validator from the controller's
        // onPlace path so a partially-broken multiblock stops producing.
        if (!MultiblockShapeValidator.validateHollowBox(block, 5, 7, 5,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA)) {
            return false;
        }
        SimpleFluidNode input = tankAbove(block);
        if (input == null) return false;
        FluidType heldType = tankFuel(input);
        if (heldType == null || !heldType.id().equals(BuiltinFluidTypes.CRUDE_OIL.id())) return false;
        SimpleFluidNode dieselOut    = tankAt(block.getRelative( 0, 0,  1));
        SimpleFluidNode gasolineOut  = tankAt(block.getRelative( 1, 0,  0));
        SimpleFluidNode lubricantOut = tankAt(block.getRelative( 0, 0, -1));
        SimpleFluidNode residueOut   = tankAt(block.getRelative(-1, 0,  0));
        if (dieselOut == null || gasolineOut == null || lubricantOut == null || residueOut == null) return false;
        // Capacity preflight: don't drain crude unless every output can accept its share.
        if (input.contents() == null || input.contents().amountMb() < REFINERY_BATCH_MB) return false;
        if (capacityFreeFor(dieselOut,    BuiltinFluidTypes.DIESEL)    < REFINERY_DIESEL)    return false;
        if (capacityFreeFor(gasolineOut,  BuiltinFluidTypes.GASOLINE)  < REFINERY_GASOLINE)  return false;
        if (capacityFreeFor(lubricantOut, BuiltinFluidTypes.LUBRICANT) < REFINERY_LUBRICANT) return false;
        if (capacityFreeFor(residueOut,   BuiltinFluidTypes.WATER)     < REFINERY_RESIDUE)   return false;
        long drawn = input.draw(REFINERY_BATCH_MB);
        if (drawn < REFINERY_BATCH_MB) {
            // Shouldn't happen given the preflight but be safe.
            return false;
        }
        dieselOut.offer(BuiltinFluidTypes.DIESEL, REFINERY_DIESEL);
        gasolineOut.offer(BuiltinFluidTypes.GASOLINE, REFINERY_GASOLINE);
        lubricantOut.offer(BuiltinFluidTypes.LUBRICANT, REFINERY_LUBRICANT);
        residueOut.offer(BuiltinFluidTypes.WATER, REFINERY_RESIDUE);
        node.draw(REFINERY_DRAW);
        return true;
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
