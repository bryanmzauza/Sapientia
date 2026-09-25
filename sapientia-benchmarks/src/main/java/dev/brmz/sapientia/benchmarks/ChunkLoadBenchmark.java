package dev.brmz.sapientia.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Main-thread cost of applying one loaded chunk with 1,000 Sapientia blocks
 * (the target in {@code docs/jogabilidade.md} section 9.3 is 1 ms): the block
 * index, machine registration in the engine, and the energy nodes joining the
 * graph, followed by the matching unload. Half the blocks are machines, half
 * cables. The database read itself happens on the database thread and is not
 * part of this cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
@Fork(1)
@State(Scope.Benchmark)
public class ChunkLoadBenchmark {

    private static final int BLOCKS = 1_000;
    private static final Logger LOGGER = Logger.getLogger("ChunkLoadBenchmark");

    private ChunkBlockIndex index;
    private NetworkGraph graph;
    private final List<CustomBlockStore.StoredBlock> stored = new ArrayList<>(BLOCKS);
    private final List<SimpleEnergyNode> nodes = new ArrayList<>(BLOCKS);

    @Setup(Level.Trial)
    public void setUp() {
        SapientiaBlock machine = block("machine");
        SapientiaBlock cable = block("cable");
        SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
        registry.register(machine);
        registry.register(cable);
        index = new ChunkBlockIndex(LOGGER, new CustomBlockStore(LOGGER, null), registry);
        SapientiaEngine engine = new SapientiaEngine(LOGGER, new EngineConfig(5, 4, 600, ChunkLimits.DEFAULT));
        engine.registerBehavior(machine.id(), 20, context -> 20);
        index.addObserver(engine);
        graph = new NetworkGraph();
        for (int i = 0; i < BLOCKS; i++) {
            // Rows of alternating machines and cables filling a 16 x 16 footprint, four layers high.
            BlockKey key = new BlockKey("world", i % 16, 64 + i / 256, (i / 16) % 16);
            boolean isMachine = i % 2 == 0;
            stored.add(new CustomBlockStore.StoredBlock(key, (isMachine ? machine : cable).id().toString(), null));
            nodes.add(new SimpleEnergyNode(UUID.randomUUID(), key,
                    isMachine ? EnergyNodeType.CONSUMER : EnergyNodeType.CABLE, EnergyTier.LOW, 0, 8));
        }
    }

    @Benchmark
    public void loadAndUnload(Blackhole blackhole) {
        long token = index.beginLoad("world", 0, 0);
        index.apply("world", 0, 0, token, stored);
        for (SimpleEnergyNode node : nodes) {
            graph.addNode(node);
        }
        blackhole.consume(graph.networkCount());
        index.unloadChunk("world", 0, 0);
        blackhole.consume(graph.removeChunk("world", 0, 0));
    }

    private static SapientiaBlock block(String name) {
        NamespacedKey id = new NamespacedKey("sapientia", name);
        return new SapientiaBlock() {
            @Override public @NotNull NamespacedKey id() { return id; }
            @Override public @NotNull Material baseMaterial() { return Material.STONE; }
            @Override public @NotNull String displayNameKey() { return "block." + name + ".name"; }
        };
    }
}
