package dev.brmz.sapientia.core.logic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable execution scratchpad shared across the lifetime of a single
 * {@link CompiledProgram} run by {@link LogicRunner} (T-302 / 1.3.0).
 *
 * <p>Memory is carried across ticks by {@code LogicServiceImpl} — it survives the
 * tick boundary so {@code memory_write} in tick N is visible to {@code memory_read}
 * in tick N+1. Memory is per-program and is NOT persisted across plugin restarts.
 */
public final class LogicContext {

    private final String programName;
    private final Map<String, LogicValue> memory;
    private long tick;
    private final Map<String, String> log = new LinkedHashMap<>();

    public LogicContext(@NotNull String programName, @NotNull Map<String, LogicValue> memory, long tick) {
        this.programName = programName;
        this.memory = new HashMap<>(memory);
        this.tick = tick;
    }

    public @NotNull String programName() {
        return programName;
    }

    public long tick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public @Nullable LogicValue readMemory(@NotNull String key) {
        return memory.get(key);
    }

    public void writeMemory(@NotNull String key, @NotNull LogicValue value) {
        memory.put(key, value);
    }

    public @NotNull Map<String, LogicValue> memorySnapshot() {
        return Map.copyOf(memory);
    }

    /** Buffered log entry for a {@code log} node — {@code key} is the node id. */
    public void log(@NotNull String nodeId, @NotNull String message) {
        log.put(nodeId, message);
    }

    public @NotNull Map<String, String> drainLog() {
        Map<String, String> snapshot = Map.copyOf(log);
        log.clear();
        return snapshot;
    }
}
