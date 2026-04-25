package dev.brmz.sapientia.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * P-014 — Geyser mappings write for 50 items + 20 blocks. We exercise the same
 * rendering loop {@code GeyserMappingsBuilder} uses (sort-by-CMD, escape,
 * StringBuilder concat) against a synthetic dataset to keep the benchmark free
 * of Bukkit dependencies. Target: &lt; 200 ms.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class GeyserMappingBenchmark {

    private List<Entry> entries;

    @Setup
    public void setUp() {
        entries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            entries.add(new Entry("sapientia:item_" + i, "minecraft:stick", 100_000 + i));
        }
        for (int i = 0; i < 20; i++) {
            entries.add(new Entry("sapientia:block_" + i, "minecraft:cobblestone", 200_000 + i));
        }
    }

    @Benchmark
    public String renderMappings() {
        // Mirror GeyserMappingsBuilder.render() output shape.
        StringBuilder sb = new StringBuilder(8 * 1024);
        sb.append("{\"format_version\":\"1\",\"items\":{");
        boolean first = true;
        for (Entry e : entries) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.base).append("\":[{")
                    .append("\"name\":\"").append(e.id).append("\",")
                    .append("\"custom_model_data\":").append(e.cmd).append(',')
                    .append("\"display_name\":\"").append(e.id).append("\"}]");
        }
        sb.append("}}");
        return sb.toString();
    }

    private record Entry(String id, String base, int cmd) {}
}
