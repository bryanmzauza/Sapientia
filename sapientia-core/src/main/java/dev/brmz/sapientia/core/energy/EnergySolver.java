package dev.brmz.sapientia.core.energy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.events.SapientiaEnergyFlowEvent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Energy solver. Each cycle, for every network that is due:
 * <ol>
 *   <li>Generators produce {@code generationPerTick(tier)} into their own buffer;
 *       what does not fit spills into capacitors.</li>
 *   <li>Consumers are topped up by up to {@code consumptionPerTick(tier)}, taken
 *       from capacitors first and then from generators. Machines spend energy
 *       from their consumer's buffer.</li>
 *   <li>A {@link SapientiaEnergyFlowEvent} reports the totals on the main thread
 *       (only built while some plugin listens to it).</li>
 * </ol>
 *
 * <p>Only generators, capacitors and consumers are visited, never cables. A
 * network where no energy moved goes to sleep until something outside the
 * solver changes one of its buffers ({@link SimpleEnergyNode#offer},
 * {@link SimpleEnergyNode#draw}), its topology changes, or one of its chunks
 * becomes active again. Nodes in inactive chunks are skipped.
 *
 * <p>With {@link #runOn} the cycle runs on another thread from role snapshots
 * published by the main thread; buffers are atomic and events are handed back
 * to the main thread. Without it, {@link #tick} runs the cycle in place.
 */
public final class EnergySolver {

    private final NetworkGraph graph;
    private final Consumer<SapientiaEnergyFlowEvent> events;
    private final BooleanSupplier hasListeners;
    private volatile boolean reportFlows = true;
    private final ConcurrentLinkedQueue<SapientiaEnergyFlowEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private @Nullable Executor executor;
    private @Nullable Logger logger;
    private long cycle;

    /** Fires flow events through Bukkit, and builds them only while some plugin listens. */
    public EnergySolver(@NotNull NetworkGraph graph) {
        this(graph, event -> Bukkit.getPluginManager().callEvent(event),
                () -> SapientiaEnergyFlowEvent.getHandlerList().getRegisteredListeners().length > 0);
    }

    public EnergySolver(@NotNull NetworkGraph graph, @NotNull Consumer<SapientiaEnergyFlowEvent> events) {
        this(graph, events, () -> true);
    }

    private EnergySolver(@NotNull NetworkGraph graph, @NotNull Consumer<SapientiaEnergyFlowEvent> events,
                         @NotNull BooleanSupplier hasListeners) {
        this.graph = graph;
        this.events = events;
        this.hasListeners = hasListeners;
    }

    /** Runs solver cycles on {@code executor} instead of the calling thread. */
    public void runOn(@NotNull Executor executor, @NotNull Logger logger) {
        this.executor = executor;
        this.logger = logger;
    }

    /**
     * Main thread, once per energy period: fires the events of the last cycle,
     * publishes topology changes and starts the next cycle. If the previous
     * cycle is still running, this period is skipped rather than queued.
     */
    public void tick() {
        publishEvents();
        reportFlows = hasListeners.getAsBoolean();
        graph.refreshSnapshots();
        Executor exec = executor;
        if (exec == null) {
            runCycle();
            publishEvents();
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        exec.execute(() -> {
            try {
                runCycle();
            } catch (RuntimeException e) {
                if (logger != null) logger.log(Level.SEVERE, "Energy solver cycle failed", e);
            } finally {
                running.set(false);
            }
        });
    }

    private void publishEvents() {
        SapientiaEnergyFlowEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (event.network() instanceof NetworkGraph.Network n && n.isRemoved()) continue;
            events.accept(event);
        }
    }

    private void runCycle() {
        long c = ++cycle;
        for (NetworkGraph.Network network : graph.collectDue(c)) {
            List<SimpleEnergyNode> generators = network.roleSnapshot(NetworkGraph.GENERATORS);
            if (generators == null) {
                graph.retry(network, c, 1); // created after the last snapshot
                continue;
            }
            if (solve(network, generators,
                    network.roleSnapshot(NetworkGraph.CAPACITORS),
                    network.roleSnapshot(NetworkGraph.CONSUMERS))) {
                graph.worked(network, c);
            } else {
                graph.idle(network, c, 0);
            }
        }
    }

    private boolean solve(NetworkGraph.Network network, List<SimpleEnergyNode> generators,
                          List<SimpleEnergyNode> capacitors, List<SimpleEnergyNode> consumers) {
        // 1. Generation, spilling into capacitors.
        long generated = 0;
        for (SimpleEnergyNode g : generators) {
            if (!g.isActive()) continue;
            long produced = EnergySpecs.generationPerTick(g.tier());
            long inserted = g.fill(produced);
            generated += inserted;
            long overflow = produced - inserted;
            for (SimpleEnergyNode c : capacitors) {
                if (overflow <= 0) break;
                if (!c.isActive()) continue;
                long spilled = c.fill(overflow);
                overflow -= spilled;
                generated += spilled;
            }
        }

        // 2. Top up consumers from capacitors, then generators.
        long consumed = 0;
        for (SimpleEnergyNode cons : consumers) {
            if (!cons.isActive()) continue;
            long need = Math.min(EnergySpecs.consumptionPerTick(cons.tier()),
                    cons.bufferMax() - cons.bufferCurrent());
            if (need <= 0) continue;
            long took = take(capacitors, need);
            took += take(generators, need - took);
            if (took <= 0) continue;
            long accepted = cons.fill(took);
            if (accepted < took) {
                giveBack(capacitors, generators, took - accepted);
            }
            consumed += accepted;
        }

        if (generated <= 0 && consumed <= 0) {
            return false;
        }
        if (!reportFlows) {
            return true;
        }
        long stored = 0;
        for (SimpleEnergyNode n : generators) stored += n.bufferCurrent();
        for (SimpleEnergyNode n : capacitors) stored += n.bufferCurrent();
        for (SimpleEnergyNode n : consumers) stored += n.bufferCurrent();
        pendingEvents.add(new SapientiaEnergyFlowEvent(network, generated, consumed, stored));
        return true;
    }

    private static long take(List<SimpleEnergyNode> sources, long amount) {
        long took = 0;
        for (SimpleEnergyNode source : sources) {
            if (took >= amount) break;
            if (!source.isActive()) continue;
            took += source.drain(amount - took);
        }
        return took;
    }

    private static void giveBack(List<SimpleEnergyNode> capacitors, List<SimpleEnergyNode> generators, long amount) {
        for (SimpleEnergyNode n : capacitors) {
            if (amount <= 0) return;
            amount -= n.fill(amount);
        }
        for (SimpleEnergyNode n : generators) {
            if (amount <= 0) return;
            amount -= n.fill(amount);
        }
    }
}
