package dev.brmz.sapientia.core.logistics;

import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Operator-facing logistics configuration (T-444 / T-445 / 1.8.1).
 *
 * <p>Reads the {@code network.solver} key from {@code config.yml}:
 * <ul>
 *   <li>{@code legacy} (default) — keeps the 1.1.0 greedy solver in
 *       {@link ItemSolver}. Best for small / mid-sized item networks
 *       (≤ 100 nodes) and existing worlds.</li>
 *   <li>{@code maxflow} — opts the network in to the
 *       {@link MaxFlowItemSolver} (Edmonds-Karp). Targeted at HV+ networks
 *       with multiple producers and consumers where the greedy first-pass
 *       under-utilises bottlenecks. See ADR-020.</li>
 * </ul>
 *
 * <p>Pure POJO so it can be unit-tested without Bukkit.
 */
public final class LogisticsConfig {

    /** Available solver strategies. */
    public enum Solver {
        LEGACY,
        MAXFLOW;

        /**
         * Parse the {@code network.solver} string. Unknown values fall
         * back to {@link #LEGACY} so a typo never breaks routing.
         */
        public static @NotNull Solver parse(@Nullable String value) {
            if (value == null) return LEGACY;
            String normalised = value.trim().toLowerCase(Locale.ROOT);
            return switch (normalised) {
                case "maxflow", "max-flow", "max_flow" -> MAXFLOW;
                default -> LEGACY;
            };
        }
    }

    private final Solver solver;

    public LogisticsConfig(@NotNull Solver solver) {
        this.solver = solver;
    }

    public @NotNull Solver solver() {
        return solver;
    }

    /** Build the default config (legacy solver). */
    public static @NotNull LogisticsConfig defaults() {
        return new LogisticsConfig(Solver.LEGACY);
    }

    /**
     * Read the config from a Bukkit {@link FileConfiguration}. Missing
     * sections / keys silently fall back to defaults.
     */
    public static @NotNull LogisticsConfig from(@NotNull FileConfiguration config) {
        ConfigurationSection network = config.getConfigurationSection("network");
        if (network == null) return defaults();
        String raw = network.getString("solver");
        return new LogisticsConfig(Solver.parse(raw));
    }
}
