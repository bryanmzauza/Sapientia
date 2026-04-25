package dev.brmz.sapientia.core.energy;

import java.util.ArrayList;
import java.util.List;

import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.events.SapientiaEnergyFlowEvent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Greedy proportional energy solver (T-142). Each tick:
 * <ol>
 *   <li>Generators contribute {@code generationPerTick(tier)} into their own buffer.</li>
 *   <li>Excess generator energy spills into capacitors.</li>
 *   <li>Each consumer drains {@code consumptionPerTick(tier)}, pulling first from its
 *       own buffer, then capacitors, then generators (round-robin proportional).</li>
 *   <li>A {@link SapientiaEnergyFlowEvent} is fired per network with the totals.</li>
 * </ol>
 *
 * <p>This is intentionally O(N) per network (no max-flow yet); it is enough for the
 * 0.3.0 demo and well within the P-003 envelope on graphs of a few hundred nodes.
 * A real Ford-Fulkerson pass replaces the proportional draw step in 1.1.0.
 */
public final class EnergySolver {

    private final NetworkGraph graph;

    public EnergySolver(@NotNull NetworkGraph graph) {
        this.graph = graph;
    }

    public void tick() {
        for (EnergyNetwork network : graph.networks()) {
            tickNetwork(network);
        }
    }

    private void tickNetwork(EnergyNetwork network) {
        List<SimpleEnergyNode> generators = new ArrayList<>();
        List<SimpleEnergyNode> capacitors = new ArrayList<>();
        List<SimpleEnergyNode> consumers = new ArrayList<>();
        for (SimpleEnergyNode n : graph.membersOf(network)) {
            switch (n.type()) {
                case GENERATOR -> generators.add(n);
                case CAPACITOR -> capacitors.add(n);
                case CONSUMER  -> consumers.add(n);
                case CABLE     -> { /* transit only */ }
            }
        }

        // 1. Generation
        long generated = 0;
        for (SimpleEnergyNode g : generators) {
            long produced = EnergySpecs.generationPerTick(g.tier());
            long inserted = g.offer(produced);
            generated += inserted;
            // Spill from full generator into capacitors
            long overflow = produced - inserted;
            for (SimpleEnergyNode c : capacitors) {
                if (overflow <= 0) break;
                overflow -= c.offer(overflow);
            }
            generated += (produced - inserted) - overflow; // amount that landed in capacitors
        }

        // 2. Consumption — pull from capacitors first, then directly from generators.
        long consumed = 0;
        for (SimpleEnergyNode cons : consumers) {
            long need = EnergySpecs.consumptionPerTick(cons.tier());
            long took = 0;
            for (SimpleEnergyNode cap : capacitors) {
                if (need <= 0) break;
                long got = cap.draw(need);
                need -= got;
                took += got;
            }
            for (SimpleEnergyNode g : generators) {
                if (need <= 0) break;
                long got = g.draw(need);
                need -= got;
                took += got;
            }
            consumed += took;
            cons.markDirty();
        }

        long stored = network.totalStored();
        Bukkit.getPluginManager().callEvent(
                new SapientiaEnergyFlowEvent(network, generated, consumed, stored));
    }
}
