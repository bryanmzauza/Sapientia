package dev.brmz.sapientia.core.fluids;

import java.util.ArrayList;
import java.util.List;

import dev.brmz.sapientia.api.events.SapientiaFluidFlowEvent;
import dev.brmz.sapientia.api.events.SapientiaFluidTransferEvent;
import dev.brmz.sapientia.api.fluids.FluidNetwork;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidSpecs;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Per-tick fluid solver (T-301 / 1.2.0). For every network:
 *
 * <ol>
 *   <li>Each {@code PUMP} extracts up to {@code throughputPerTick} mB from an
 *       adjacent vanilla source and offers it to the network's tanks (greedy,
 *       first-fit).</li>
 *   <li>Each {@code DRAIN} draws up to {@code throughputPerTick} mB from any
 *       compatible tank and deposits it into an adjacent vanilla sink (cauldron
 *       or replaceable air).</li>
 * </ol>
 *
 * <p>No mixing — a tank pre-bound to fluid A refuses fluid B. Empty tanks adopt
 * the first fluid offered. Continuous volume is preserved each tick.
 */
public final class FluidSolver {

    private final FluidNetworkGraph graph;
    private final FluidServiceImpl service;

    public FluidSolver(@NotNull FluidNetworkGraph graph, @NotNull FluidServiceImpl service) {
        this.graph = graph;
        this.service = service;
    }

    public void tick() {
        FluidType water = service.type(BuiltinFluidTypes.WATER.id()).orElse(BuiltinFluidTypes.WATER);
        FluidType lava = service.type(BuiltinFluidTypes.LAVA.id()).orElse(BuiltinFluidTypes.LAVA);

        for (FluidNetwork network : graph.networks()) {
            List<SimpleFluidNode> tanks = new ArrayList<>();
            List<SimpleFluidNode> pumps = new ArrayList<>();
            List<SimpleFluidNode> drains = new ArrayList<>();
            for (SimpleFluidNode n : graph.membersOf(network)) {
                switch (n.type()) {
                    case TANK -> tanks.add(n);
                    case PUMP -> pumps.add(n);
                    case DRAIN -> drains.add(n);
                    default -> { /* PIPE / JUNCTION are passive */ }
                }
            }
            if (tanks.isEmpty()) continue;

            long pumped = 0L;
            long drained = 0L;

            for (SimpleFluidNode pump : pumps) {
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

            for (SimpleFluidNode drain : drains) {
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

            long buffered = 0L;
            for (SimpleFluidNode tank : tanks) {
                FluidStack c = (FluidStack) tank.contents();
                if (c != null) buffered += c.amountMb();
            }
            if (pumped > 0L || drained > 0L) {
                Bukkit.getPluginManager().callEvent(
                        new SapientiaFluidFlowEvent(network, pumped, drained, buffered));
            }
        }
    }

    /** Returns total mB accepted by tanks for the given fluid, summed greedy first-fit. */
    private long offerToTanks(List<SimpleFluidNode> tanks, FluidType type, long amount,
                              SimpleFluidNode source) {
        long remaining = amount;
        for (SimpleFluidNode tank : tanks) {
            if (remaining <= 0L) break;
            long taken = tank.offer(type, remaining);
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
            long pulled = tank.draw(Math.min(amount, c.amountMb()));
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
