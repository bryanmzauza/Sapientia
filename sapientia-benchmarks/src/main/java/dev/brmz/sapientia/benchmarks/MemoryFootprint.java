package dev.brmz.sapientia.benchmarks;

import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.block.CustomBlockStore;
import dev.brmz.sapientia.core.block.SapientiaBlockRegistry;
import dev.brmz.sapientia.core.energy.NetworkGraph;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.engine.ChunkLimits;
import dev.brmz.sapientia.core.engine.EngineConfig;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Heap cost per block of the main in-memory structures, measured with one
 * million entries each: the block index (every Sapientia block), a cable in
 * the energy graph, and a machine in the engine. Compare with the memory
 * targets in {@code docs/jogabilidade.md} section 9.3.
 *
 * <p>Run with {@code ./gradlew :sapientia-benchmarks:footprint}.
 */
public final class MemoryFootprint {

    private static final int COUNT = 1_000_000;
    private static final Logger LOGGER = Logger.getLogger("MemoryFootprint");

    private MemoryFootprint() {}

    public static void main(String[] args) {
        SapientiaBlock cable = block();
        SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
        registry.register(cable);

        long base = usedHeap();
        ChunkBlockIndex index = new ChunkBlockIndex(LOGGER, new CustomBlockStore(LOGGER, null), registry);
        for (int i = 0; i < COUNT; i++) index.put(key(i), cable);
        long afterIndex = usedHeap();

        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < COUNT; i++) {
            graph.addNode(new SimpleEnergyNode(UUID.randomUUID(), key(i), EnergyNodeType.CABLE, EnergyTier.LOW, 0, 1));
        }
        long afterGraph = usedHeap();

        SapientiaEngine engine = new SapientiaEngine(LOGGER, new EngineConfig(5, 4, 600, ChunkLimits.DEFAULT));
        engine.registerBehavior(cable.id(), 20, context -> 100);
        for (int i = 0; i < COUNT; i++) engine.onBlockAdded(key(i), cable);
        long afterEngine = usedHeap();

        report("Block index, per block", afterIndex - base);
        report("Energy graph, per cable", afterGraph - afterIndex);
        report("Engine, per machine", afterEngine - afterGraph);
        // Keep everything reachable until the last measurement.
        if (index.size() + graph.nodeCount() + engine.scheduler().registeredCount() == 0) {
            throw new AssertionError();
        }
    }

    /** Spreads entries over chunks the way a dense build would fill them. */
    private static BlockKey key(int i) {
        return new BlockKey("world", i % 1000, 64 + (i / 1000) % 100, i / 100_000);
    }

    private static void report(String what, long bytes) {
        System.out.printf(Locale.ROOT, "%-26s %6.1f bytes%n", what, bytes / (double) COUNT);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < 5; i++) {
            System.gc();
        }
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static SapientiaBlock block() {
        NamespacedKey id = new NamespacedKey("sapientia", "cable");
        return new SapientiaBlock() {
            @Override public @NotNull NamespacedKey id() { return id; }
            @Override public @NotNull Material baseMaterial() { return Material.STONE; }
            @Override public @NotNull String displayNameKey() { return "block.cable.name"; }
        };
    }
}
