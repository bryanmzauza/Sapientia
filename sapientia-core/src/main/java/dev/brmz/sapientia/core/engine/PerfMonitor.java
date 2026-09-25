package dev.brmz.sapientia.core.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Per-subsystem timing for {@code /sapientia perf}. Each section keeps a
 * moving average of milliseconds per tick and the worst tick of the last
 * {@value #WINDOW_TICKS} ticks.
 */
public final class PerfMonitor {

    public static final int WINDOW_TICKS = 100;
    private static final double ALPHA = 0.05;

    /** Snapshot of one section. */
    public record Section(@NotNull String name, double averageMs, double peakMs) {}

    private static final class Stat {
        long pendingNanos;
        double averageMs;
        long windowPeakNanos;
        long lastWindowPeakNanos;
    }

    private final Map<String, Stat> stats = new LinkedHashMap<>();
    private final Stat total = new Stat();
    private long ticks;

    /** Adds time spent in a section during the current tick. */
    public void record(@NotNull String section, long nanos) {
        stats.computeIfAbsent(section, s -> new Stat()).pendingNanos += nanos;
    }

    /** Closes the current tick: folds pending time into averages and peaks. */
    public void endTick() {
        long tickTotal = 0;
        for (Stat stat : stats.values()) {
            fold(stat, stat.pendingNanos);
            tickTotal += stat.pendingNanos;
            stat.pendingNanos = 0;
        }
        fold(total, tickTotal);
        if (++ticks % WINDOW_TICKS == 0) {
            for (Stat stat : stats.values()) roll(stat);
            roll(total);
        }
    }

    private static void fold(Stat stat, long nanos) {
        double ms = nanos / 1_000_000.0;
        stat.averageMs = stat.averageMs == 0 ? ms : stat.averageMs + ALPHA * (ms - stat.averageMs);
        stat.windowPeakNanos = Math.max(stat.windowPeakNanos, nanos);
    }

    private static void roll(Stat stat) {
        stat.lastWindowPeakNanos = stat.windowPeakNanos;
        stat.windowPeakNanos = 0;
    }

    public @NotNull Section total() {
        return snapshot("total", total);
    }

    public @NotNull Map<String, Section> sections() {
        Map<String, Section> out = new LinkedHashMap<>();
        stats.forEach((name, stat) -> out.put(name, snapshot(name, stat)));
        return Collections.unmodifiableMap(out);
    }

    private static Section snapshot(String name, Stat stat) {
        long peak = Math.max(stat.windowPeakNanos, stat.lastWindowPeakNanos);
        return new Section(name, stat.averageMs, peak / 1_000_000.0);
    }
}
