package dev.brmz.sapientia.core.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.events.SapientiaLogicTickEvent;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicService;
import dev.brmz.sapientia.api.logic.LogicValue;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Owns the runtime registry of logic programs (T-302 / 1.3.0). Programs are
 * compiled on registration, persisted as YAML and ticked once per scheduling
 * pass via {@link #tickAll()}.
 *
 * <p>Memory snapshots are kept per program across ticks; a program reload
 * (re-registration) clears its memory.
 */
public final class LogicServiceImpl implements LogicService {

    private final Logger logger;
    private final LogicProgramStore store;
    private final LogicCompiler compiler = new LogicCompiler();
    private final LogicRunner runner = new LogicRunner();
    private final LogicYaml yaml = new LogicYaml();

    private final ConcurrentMap<String, Entry> programs = new ConcurrentHashMap<>();
    private long globalTick;

    public LogicServiceImpl(@NotNull Logger logger, @NotNull LogicProgramStore store) {
        this.logger = logger;
        this.store = store;
    }

    /** Loads every persisted program at plugin startup. Called by {@code SapientiaPlugin}. */
    public void hydrate() {
        for (LogicProgramStore.StoredProgram stored : store.loadAll()) {
            try {
                LogicProgram program = yaml.parse(stored.yamlSource());
                CompiledProgram compiled = compiler.compile(program);
                programs.put(program.name(), new Entry(program, compiled, stored.enabled(), new HashMap<>()));
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING,
                        "Skipping invalid stored logic program '" + stored.name() + "': " + ex.getMessage());
                store.upsert(stored.name(), stored.yamlSource(), false, ex.getMessage());
            }
        }
    }

    @Override
    public void register(@NotNull LogicProgram program) {
        CompiledProgram compiled = compiler.compile(program); // throws on cycle / unknown kind
        Entry prior = programs.get(program.name());
        boolean enabled = prior == null || prior.enabled;
        programs.put(program.name(), new Entry(program, compiled, enabled, new HashMap<>()));
        store.upsert(program.name(), yaml.dump(program), enabled, null);
    }

    @Override
    public void unregister(@NotNull String name) {
        programs.remove(name);
        store.delete(name);
    }

    @Override
    public @NotNull List<String> list() {
        return new ArrayList<>(new TreeMap<>(programs).keySet());
    }

    @Override
    public @NotNull Optional<LogicProgram> get(@NotNull String name) {
        Entry e = programs.get(name);
        return e == null ? Optional.empty() : Optional.of(e.program);
    }

    @Override
    public void setEnabled(@NotNull String name, boolean enabled) {
        Entry e = programs.get(name);
        if (e == null) return;
        e.enabled = enabled;
        store.upsert(name, yaml.dump(e.program), enabled, null);
    }

    @Override
    public boolean isEnabled(@NotNull String name) {
        Entry e = programs.get(name);
        return e != null && e.enabled;
    }

    @Override
    public void runOnce(@NotNull String name) {
        Entry e = programs.get(name);
        if (e == null) {
            throw new IllegalArgumentException("unknown logic program: " + name);
        }
        runEntry(name, e);
    }

    @Override
    public @NotNull String exportYaml(@NotNull String name) {
        Entry e = programs.get(name);
        if (e == null) {
            throw new IllegalArgumentException("unknown logic program: " + name);
        }
        return yaml.dump(e.program);
    }

    @Override
    public @NotNull LogicProgram importYaml(@NotNull String text) {
        return yaml.parse(text);
    }

    /** Drives one execution pass for every enabled program. Wired into the tick scheduler. */
    public void tickAll() {
        globalTick++;
        for (Map.Entry<String, Entry> entry : programs.entrySet()) {
            Entry e = entry.getValue();
            if (!e.enabled) continue;
            runEntry(entry.getKey(), e);
        }
    }

    private void runEntry(String name, Entry e) {
        SapientiaLogicTickEvent event = new SapientiaLogicTickEvent(name, e.executionCount);
        try {
            Bukkit.getPluginManager().callEvent(event);
        } catch (IllegalStateException ignored) {
            // Bukkit may not be initialised in tests — fall through and run anyway.
        }
        if (event.isCancelled()) {
            return;
        }
        LogicContext ctx = new LogicContext(name, e.memory, globalTick);
        try {
            runner.run(e.compiled, ctx);
            // Persist memory snapshot in-place for next tick.
            e.memory.clear();
            e.memory.putAll(ctx.memorySnapshot());
            Map<String, String> log = ctx.drainLog();
            if (!log.isEmpty()) {
                for (Map.Entry<String, String> line : log.entrySet()) {
                    logger.info("[logic " + name + "/" + line.getKey() + "] " + line.getValue());
                }
            }
            e.executionCount++;
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Logic program '" + name + "' failed: " + ex.getMessage(), ex);
            e.enabled = false;
            store.upsert(name, yaml.dump(e.program), false, ex.getMessage());
        }
    }

    private static final class Entry {
        final LogicProgram program;
        final CompiledProgram compiled;
        boolean enabled;
        final Map<String, LogicValue> memory;
        long executionCount;

        Entry(LogicProgram program, CompiledProgram compiled, boolean enabled, Map<String, LogicValue> memory) {
            this.program = program;
            this.compiled = compiled;
            this.enabled = enabled;
            this.memory = memory;
        }
    }
}
