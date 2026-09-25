package dev.brmz.sapientia.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.brmz.sapientia.core.pack.bedrock.GeyserMappingsBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * P-014 — Geyser mappings render for the full built-in catalogue size
 * (~250 items spread over ~40 base items). Calls the real
 * {@link GeyserMappingsBuilder#render}, which is free of Bukkit dependencies.
 * Target: &lt; 200 ms.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class GeyserMappingBenchmark {

    private List<GeyserMappingsBuilder.Entry> entries;

    @Setup
    public void setUp() {
        entries = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            entries.add(new GeyserMappingsBuilder.Entry(
                    "sapientia:item_" + i, "minecraft:base_" + (i % 40), "Item " + i));
        }
    }

    @Benchmark
    public String renderMappings() {
        return GeyserMappingsBuilder.render(entries);
    }
}
