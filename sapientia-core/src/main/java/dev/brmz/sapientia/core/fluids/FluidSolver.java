package dev.brmz.sapientia.core.fluids;

import java.util.ArrayList;
import java.util.List;

import dev.brmz.sapientia.api.events.SapientiaFluidFlowEvent;
import dev.brmz.sapientia.api.events.SapientiaFluidTransferEvent;
import dev.brmz.sapientia.api.fluids.FluidSpecs;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Fluid solver. Each cycle, for every network that is due:
 *
 * <ol>
 *   <li>Each active {@code PUMP} extracts up to {@code throughputPerTick} mB
 *       from an adjacent vanilla source and offers it to the network's tanks
 *       (greedy, first fit).</li>
 *   <li>Each active {@code DRAIN} draws up to {@code throughputPerTick} mB from
 *       a tank and deposits it into an adjacent vanilla sink (cauldron or
 *       replaceable air).</li>
 * </ol>
 *
 * <p>No mixing: a tank holding fluid A refuses fluid B, and an empty tank
 * adopts the first fluid offered. Runs on the main thread (it touches the
 * world). A network where nothing moved backs off exponentially, up to
 * {@link #MAX_IDLE_CYCLES} cycles, and wakes early when a machine fills or
 * drains one of its tanks; one without active tanks sleeps until its topology
 * changes or one of its chunks becomes active.
 */
public final class FluidSolver {

    /** Longest back-off of an idle network, in cycles (the fluid cycle is five ticks). */
    public static final int MAX_IDLE_CYCLES = 8;

    private final FluidNetworkGraph graph;
    private final FluidServiceImpl service;
    private long cycle;

    public FluidSolver(@NotNull FluidNetworkGraph graph, @NotNull FluidServiceImpl service) {
        this.graph = graph;
        this.service = service;
    }

    public void tick() {
        FluidType water = service.type(BuiltinFluidTypes.WATER.id()).orElse(BuiltinFluidTypes.WATER);
        FluidType lava = service.type(BuiltinFluidTypes.LAVA.id()).orElse(BuiltinFluidTypes.LAVA);
        long c = ++cycle;
        for (FluidNetworkGraph.Network network : graph.collectDue(c)) {
            if (network.isRemoved()) continue; // an event listener changed the topology
            List<SimpleFluidNode> tanks = active(network.role(FluidNetworkGraph.TANKS));
            if (tanks.isEmpty()) {
                graph.idle(network, c, 0);
                continue;
            }
            if (tickNetwork(network, tanks, water, lava)) {
                graph.worked(network, c);
            } else {
                graph.idle(network, c, MAX_IDLE_CYCLES);
            }
        }
    }

    private boolean tickNetwork(FluidNetworkGraph.Network network, List<SimpleFluidNode> tanks,
                                FluidType water, FluidType lava) {
        long pumped = 0L;
        long drained = 0L;

        for (SimpleFluidNode pump : network.role(FluidNetworkGraph.PUMPS)) {
            if (!pump.isActive()) continue;
            long throughput = FluidSpecs.throughputPerTick(pump.tier());
            Block origin = pump.block();
            if (origin == null) continue;
            AdjacentFluids.Extract source = AdjacentFluids.peekSource(origin, null, water, lava);
            if (source == null) continue;
            long want = Math.min(throughput, source.amountMb());
            long offered = offerToTanks(tanks, source.type(), want, pump);
            if (offered <= 0L) continue;
            AdjacentFluids.consumeFromSource(source.source(), offered);
            pumped += offered;
        }

        for (SimpleFluidNode drain : network.role(FluidNetworkGraph.DRAINS)) {
            if (!drain.isActive()) continue;
            long throughput = FluidSpecs.throughputPerTick(drain.tier());
            Block origin = drain.block();
            if (origin == null) continue;
            FluidStack pulled = drawFromTanks(tanks, throughput, drain);
            if (pulled == null || pulled.isEmpty()) continue;
            long placed = AdjacentFluids.deposit(origin, pulled, water, lava);
            long unused = pulled.amountMb() - placed;
            if (unused > 0L) {
                // Return unused volume to tanks (best effort).
                offerToTanks(tanks, pulled.type(), unused, drain);
            }
            drained += placed;
        }

        if (pumped <= 0L && drained <= 0L) {
            return false;
        }
        long buffered = 0L;
        for (SimpleFluidNode tank : tanks) {
            FluidStack c = tank.contents();
            if (c != null) buffered += c.amountMb();
        }
        Bukkit.getPluginManager().callEvent(
                new SapientiaFluidFlowEvent(network, pumped, drained, buffered));
        return true;
    }

    private static List<SimpleFluidNode> active(List<SimpleFluidNode> nodes) {
        List<SimpleFluidNode> out = new ArrayList<>(nodes.size());
        for (SimpleFluidNode n : nodes) {
            if (n.isActive()) out.add(n);
        }
        return out;
    }

    /** Returns total mB accepted by tanks for the given fluid, summed greedy first-fit. */
    private long offerToTanks(List<SimpleFluidNode> tanks, FluidType type, long amount,
                              SimpleFluidNode source) {
        long remaining = amount;
        for (SimpleFluidNode tank : tanks) {
            if (remaining <= 0L) break;
            long taken = tank.fill(type, remaining);
            if (taken > 0L) {
                remaining -= taken;
                Bukkit.getPluginManager().callEvent(new SapientiaFluidTransferEvent(
                        source, tank, new FluidStack(type, taken), taken));
            }
        }
        return amount - remaining;
    }

    /** Draws up to {@code amount} mB from any single-typed tank. */
    private FluidStack drawFromTanks(List<SimpleFluidNode> tanks, long amount,
                                     SimpleFluidNode sink) {
        for (SimpleFluidNode tank : tanks) {
            FluidStack c = tank.contents();
            if (c == null) continue;
            long pulled = tank.drain(Math.min(amount, c.amountMb()));
            if (pulled > 0L) {
                FluidStack out = new FluidStack(c.type(), pulled);
                Bukkit.getPluginManager().callEvent(new SapientiaFluidTransferEvent(
                        tank, sink, out, pulled));
                return out;
            }
        }
        return null;
    }
}
