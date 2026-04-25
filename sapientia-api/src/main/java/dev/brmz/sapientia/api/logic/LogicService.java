package dev.brmz.sapientia.api.logic;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * Service that owns the runtime registry of {@link LogicProgram}s and drives
 * their per-tick execution (T-302 / 1.3.0). Exposed via
 * {@code SapientiaAPI#logic()}; implemented in {@code sapientia-core}.
 *
 * <p>Programs are persisted as YAML in the SQLite store ({@code logic_programs}
 * table). Loading a program through {@link #register(LogicProgram)} compiles
 * the DAG, persists the YAML source, and schedules per-tick execution when
 * the program is enabled.
 */
public interface LogicService {

    /**
     * Compiles, persists and registers the given program. If a program with the
     * same name was already registered it is replaced atomically.
     *
     * @throws IllegalArgumentException when the DAG fails compilation
     *         (cycle, unknown node ref, port collision, …).
     */
    void register(@NotNull LogicProgram program);

    /** Removes the program from the registry and the persistent store. */
    void unregister(@NotNull String name);

    /** Lists every registered program name in deterministic (lexicographic) order. */
    @NotNull List<String> list();

    /** Looks up a registered program by name. */
    @NotNull Optional<LogicProgram> get(@NotNull String name);

    /** Toggles whether the program is executed each tick. */
    void setEnabled(@NotNull String name, boolean enabled);

    /** Whether a registered program is currently enabled. */
    boolean isEnabled(@NotNull String name);

    /**
     * Runs the program once, ignoring its enabled state. Useful for debugging
     * (`/sapientia logic tick &lt;name&gt;`) and for tests.
     */
    void runOnce(@NotNull String name);

    /** Serialises the program back to YAML text (round-trip with {@link #importYaml(String)}). */
    @NotNull String exportYaml(@NotNull String name);

    /**
     * Parses a YAML document into a program. The result is not registered yet
     * — call {@link #register(LogicProgram)} after validation.
     */
    @NotNull LogicProgram importYaml(@NotNull String yaml);
}
