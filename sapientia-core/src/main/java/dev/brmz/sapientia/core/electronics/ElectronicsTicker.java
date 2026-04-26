package dev.brmz.sapientia.core.electronics;

import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.fluids.FluidNode;
import dev.brmz.sapientia.api.fluids.FluidType;
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
 * Per-tick driver for the HV / electronics / gas kinetic loop
 * (T-425 / T-426 / T-429 / 1.6.1). Mirrors {@link
 * dev.brmz.sapientia.core.petroleum.PetroleumTicker}: iterates every energy
 * node, dispatches by SapientiaBlock id and applies the per-machine physics.
 *
 * <p>Block contracts (one production tick = one call to {@link #tick()},
 * scheduled at 5L cadence by the plugin):
 *
 * <ul>
 *   <li><b>electrolyzer</b> (HV CONSUMER) — drains {@link #ELECTROLYZER_WATER_MB} mB
 *       of {@code water} from the tank above; emits {@link #ELECTROLYZER_HYDROGEN_MB}
 *       mB of {@code hydrogen} to the tank below and {@link #ELECTROLYZER_OXYGEN_MB}
 *       mB of {@code oxygen_gas} to the north-cardinal-adjacent tank (vented if
 *       absent). Burns {@link #ELECTROLYZER_ENERGY} SU. Stoichiometry mirrors
 *       2&nbsp;H&#x2082;O &rarr; 2&nbsp;H&#x2082; + O&#x2082;.</li>
 *   <li><b>boiler</b> (MV CONSUMER) — drains {@link #BOILER_WATER_MB} mB of
 *       {@code water} from above; emits {@link #BOILER_GAS_MB} mB of
 *       {@code compressed_air} to the tank below (1:2 expansion ratio
 *       represents the liquid&rarr;gas state transition). Burns
 *       {@link #BOILER_ENERGY} SU.</li>
 *   <li><b>condenser</b> (MV CONSUMER) — inverse of the boiler. Drains
 *       {@link #BOILER_GAS_MB} mB of {@code compressed_air} above; emits
 *       {@link #BOILER_WATER_MB} mB of {@code water} below. Burns
 *       {@link #CONDENSER_ENERGY} SU. The inverse-pair invariant
 *       {@code BOILER_GAS_MB / BOILER_WATER_MB == 2} is enforced by
 *       {@link dev.brmz.sapientia.core.electronics.ElectronicsTickerStoichiometryTest}.</li>
 *   <li><b>geothermal_gen</b> (HV GENERATOR) — counts the number of lava
 *       blocks among its 6 immediate neighbours; pushes
 *       {@link #GEOTHERMAL_SU_PER_LAVA} SU per neighbour into its energy buffer.</li>
 *   <li><b>gas_turbine</b> (HV GENERATOR) — burns up to {@link #GAS_TURBINE_DRAIN_MB}
 *       mB of {@code hydrogen} ({@link #HYDROGEN_SU_PER_MB} SU/mB) or
 *       {@code ethylene} ({@link #ETHYLENE_SU_PER_MB} SU/mB) from the tank below.</li>
 *   <li><b>rtg</b> (HV GENERATOR) — pushes {@link #RTG_SU_PER_TICK} SU per cycle
 *       irrespective of fuel input. Sealed isotope; no maintenance.</li>
 * </ul>
 *
 * <p>The remaining 1.6.0 HV machines ({@code rolling_mill}, {@code laser_cutter},
 * {@code chemical_reactor}, {@code gas_compressor}, {@code liquefier},
 * {@code phase_separator}) are item-based and handled by {@link
 * dev.brmz.sapientia.core.machine.MachineProcessor} via the {@link
 * dev.brmz.sapientia.api.machine.MachineRecipeRegistry}.
 */
public final class ElectronicsTicker {

    // --- electrolyzer (T-429 stoichiometry) ---
    /** Water consumed per electrolyzer cycle (mB). */
    public static final int ELECTROLYZER_WATER_MB    = 100;
    /** Hydrogen produced per cycle (mB). 2:2 ratio with water (per H atom). */
    public static final int ELECTROLYZER_HYDROGEN_MB = 200;
    /** Oxygen produced per cycle (mB). 2:1 ratio with water. */
    public static final int ELECTROLYZER_OXYGEN_MB   = 100;
    public static final long ELECTROLYZER_ENERGY     = 1024L;

    // --- boiler / condenser (gas-pressure state transition seed) ---
    public static final int  BOILER_WATER_MB = 50;
    public static final int  BOILER_GAS_MB   = 100;
    public static final long BOILER_ENERGY   = 256L;
    public static final long CONDENSER_ENERGY = 128L;

    // --- HV generators ---
    public static final long GEOTHERMAL_SU_PER_LAVA = 200L;
    public static final long HYDROGEN_SU_PER_MB     = 100L;
    public static final long ETHYLENE_SU_PER_MB     = 60L;
    public static final int  GAS_TURBINE_DRAIN_MB   = 10;
    public static final long RTG_SU_PER_TICK        = 50L;

    private final Logger logger;
    private final EnergyServiceImpl energy;
    private final FluidServiceImpl fluids;
    private final ChunkBlockIndex chunkIndex;

    private final NamespacedKey electrolyzerId;
    private final NamespacedKey boilerId;
    private final NamespacedKey condenserId;
    private final NamespacedKey geothermalId;
    private final NamespacedKey gasTurbineId;
    private final NamespacedKey rtgId;

    public ElectronicsTicker(@NotNull Logger logger,
                             @NotNull org.bukkit.plugin.Plugin plugin,
                             @NotNull EnergyServiceImpl energy,
                             @NotNull FluidServiceImpl fluids,
                             @NotNull ChunkBlockIndex chunkIndex) {
        this.logger = logger;
        this.energy = energy;
        this.fluids = fluids;
        this.chunkIndex = chunkIndex;
        this.electrolyzerId = new NamespacedKey(plugin, "electrolyzer");
        this.boilerId       = new NamespacedKey(plugin, "boiler");
        this.condenserId    = new NamespacedKey(plugin, "condenser");
        this.geothermalId   = new NamespacedKey(plugin, "geothermal_gen");
        this.gasTurbineId   = new NamespacedKey(plugin, "gas_turbine");
        this.rtgId          = new NamespacedKey(plugin, "rtg");
    }

    public void tick() {
        for (SimpleEnergyNode node : energy.graph().nodes()) {
            try {
                tickNode(node);
            } catch (RuntimeException ex) {
                logger.warning("ElectronicsTicker error at " + node.location() + ": " + ex);
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

        if (id.equals(electrolyzerId)) {
            tickElectrolyzer(node, block);
        } else if (id.equals(boilerId)) {
            tickBoiler(node, block);
        } else if (id.equals(condenserId)) {
            tickCondenser(node, block);
        } else if (id.equals(geothermalId)) {
            tickGeothermal(node, block);
        } else if (id.equals(gasTurbineId)) {
            tickGasTurbine(node, block);
        } else if (id.equals(rtgId)) {
            tickRtg(node);
        }
    }

    private void tickElectrolyzer(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < ELECTROLYZER_ENERGY) return;
        SimpleFluidNode water = tankAbove(block);
        SimpleFluidNode hydrogenOut = tankBelow(block);
        if (water == null || hydrogenOut == null) return;
        FluidType heldType = tankFuel(water);
        if (heldType == null || !heldType.id().equals(BuiltinFluidTypes.WATER.id())) return;
        if (water.contents() == null || water.contents().amountMb() < ELECTROLYZER_WATER_MB) return;
        if (capacityFreeFor(hydrogenOut, BuiltinFluidTypes.HYDROGEN) < ELECTROLYZER_HYDROGEN_MB) return;

        // Optional oxygen sink (north neighbour). Vented if absent or full.
        SimpleFluidNode oxygenOut = tankAt(block.getRelative(0, 0, -1));

        long drawn = water.draw(ELECTROLYZER_WATER_MB);
        if (drawn < ELECTROLYZER_WATER_MB) return;
        hydrogenOut.offer(BuiltinFluidTypes.HYDROGEN, ELECTROLYZER_HYDROGEN_MB);
        if (oxygenOut != null && capacityFreeFor(oxygenOut, BuiltinFluidTypes.OXYGEN_GAS) >= ELECTROLYZER_OXYGEN_MB) {
            oxygenOut.offer(BuiltinFluidTypes.OXYGEN_GAS, ELECTROLYZER_OXYGEN_MB);
        }
        node.draw(ELECTROLYZER_ENERGY);
    }

    private void tickBoiler(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < BOILER_ENERGY) return;
        SimpleFluidNode liquid = tankAbove(block);
        SimpleFluidNode gasOut = tankBelow(block);
        if (liquid == null || gasOut == null) return;
        FluidType held = tankFuel(liquid);
        if (held == null || !held.id().equals(BuiltinFluidTypes.WATER.id())) return;
        if (liquid.contents() == null || liquid.contents().amountMb() < BOILER_WATER_MB) return;
        if (capacityFreeFor(gasOut, BuiltinFluidTypes.COMPRESSED_AIR) < BOILER_GAS_MB) return;

        long drawn = liquid.draw(BOILER_WATER_MB);
        if (drawn < BOILER_WATER_MB) return;
        gasOut.offer(BuiltinFluidTypes.COMPRESSED_AIR, BOILER_GAS_MB);
        node.draw(BOILER_ENERGY);
    }

    private void tickCondenser(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.CONSUMER) return;
        if (node.bufferCurrent() < CONDENSER_ENERGY) return;
        SimpleFluidNode gasIn = tankAbove(block);
        SimpleFluidNode liquidOut = tankBelow(block);
        if (gasIn == null || liquidOut == null) return;
        FluidType held = tankFuel(gasIn);
        if (held == null || !held.id().equals(BuiltinFluidTypes.COMPRESSED_AIR.id())) return;
        if (gasIn.contents() == null || gasIn.contents().amountMb() < BOILER_GAS_MB) return;
        if (capacityFreeFor(liquidOut, BuiltinFluidTypes.WATER) < BOILER_WATER_MB) return;

        long drawn = gasIn.draw(BOILER_GAS_MB);
        if (drawn < BOILER_GAS_MB) return;
        liquidOut.offer(BuiltinFluidTypes.WATER, BOILER_WATER_MB);
        node.draw(CONDENSER_ENERGY);
    }

    private void tickGeothermal(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return;
        int lavaCount = countLavaNeighbours(block);
        if (lavaCount <= 0) return;
        long produced = Math.min(room, lavaCount * GEOTHERMAL_SU_PER_LAVA);
        if (produced <= 0L) return;
        node.offer(produced);
    }

    private void tickGasTurbine(SimpleEnergyNode node, Block block) {
        if (node.type() != EnergyNodeType.GENERATOR) return;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return;
        SimpleFluidNode tank = tankBelow(block);
        if (tank == null) return;
        FluidType fuel = tankFuel(tank);
        long suPerMb;
        if (fuel != null && fuel.id().equals(BuiltinFluidTypes.HYDROGEN.id())) {
            suPerMb = HYDROGEN_SU_PER_MB;
        } else if (fuel != null && fuel.id().equals(BuiltinFluidTypes.ETHYLENE.id())) {
            suPerMb = ETHYLENE_SU_PER_MB;
        } else {
            return;
        }
        long mbForRoom = (room + suPerMb - 1L) / suPerMb;
        long ask = Math.min((long) GAS_TURBINE_DRAIN_MB, mbForRoom);
        long burned = tank.draw(ask);
        if (burned <= 0L) return;
        node.offer(Math.min(room, burned * suPerMb));
    }

    private void tickRtg(SimpleEnergyNode node) {
        if (node.type() != EnergyNodeType.GENERATOR) return;
        long room = node.bufferMax() - node.bufferCurrent();
        if (room <= 0L) return;
        node.offer(Math.min(room, RTG_SU_PER_TICK));
    }

    /** Counts how many of the 6 immediate neighbours contain lava (any state). */
    static int countLavaNeighbours(@NotNull Block block) {
        int n = 0;
        if (block.getRelative( 1, 0, 0).getType() == Material.LAVA) n++;
        if (block.getRelative(-1, 0, 0).getType() == Material.LAVA) n++;
        if (block.getRelative( 0, 1, 0).getType() == Material.LAVA) n++;
        if (block.getRelative( 0,-1, 0).getType() == Material.LAVA) n++;
        if (block.getRelative( 0, 0, 1).getType() == Material.LAVA) n++;
        if (block.getRelative( 0, 0,-1).getType() == Material.LAVA) n++;
        return n;
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
