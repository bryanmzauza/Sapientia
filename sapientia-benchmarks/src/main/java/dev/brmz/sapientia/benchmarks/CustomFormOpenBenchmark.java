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
 * P-011 — building the in-memory representation of a {@code CustomForm} for a
 * Bedrock player. Real network send is out of scope for JMH (no Floodgate at
 * benchmark time), so this measures the part the plugin actually owns: walking
 * a fixed list of components and serialising labels/toggles into a string
 * payload. Target: &lt; 10 ms end-to-end on the reference host.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class CustomFormOpenBenchmark {

    private List<String> labels;

    @Setup
    public void setUp() {
        labels = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            labels.add("§7Energy " + i + ": " + (i * 137) + "/§a10000 §7E");
        }
    }

    @Benchmark
    public String buildPayload() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"type\":\"custom_form\",\"title\":\"Sapientia Machine\",\"content\":[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"type\":\"label\",\"text\":\"").append(labels.get(i)).append("\"}");
        }
        sb.append(",{\"type\":\"toggle\",\"text\":\"Running\",\"default\":true}]}");
        return sb.toString();
    }
}
