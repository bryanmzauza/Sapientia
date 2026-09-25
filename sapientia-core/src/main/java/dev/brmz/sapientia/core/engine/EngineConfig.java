package dev.brmz.sapientia.core.engine;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code performance} section of {@code config.yml}.
 *
 * @param tickBudgetMs          main-thread time machines may use per tick
 * @param activityRadius        chunks around each player where machines run
 * @param deactivateDelayTicks  how long a chunk stays active after the last player leaves
 * @param limits                per-chunk placement limits
 */
public record EngineConfig(double tickBudgetMs, int activityRadius, long deactivateDelayTicks,
                           @NotNull ChunkLimits limits) {

    public static final EngineConfig DEFAULT = new EngineConfig(5.0, 4, 600, ChunkLimits.DEFAULT);

    public long tickBudgetNanos() {
        return (long) (tickBudgetMs * 1_000_000L);
    }

    public static @NotNull EngineConfig from(@NotNull FileConfiguration config) {
        ConfigurationSection perf = config.getConfigurationSection("performance");
        if (perf == null) {
            return DEFAULT;
        }
        double budget = clamp(perf.getDouble("tick-budget-ms", DEFAULT.tickBudgetMs), 0.5, 40.0);
        int radius = (int) clamp(perf.getInt("activity-radius", DEFAULT.activityRadius), 1, 32);
        long delay = (long) clamp(perf.getLong("deactivate-delay-seconds", 30), 0, 3600) * 20L;

        ChunkLimits base = ChunkLimits.DEFAULT;
        ConfigurationSection lim = perf.getConfigurationSection("limits");
        ChunkLimits limits = base;
        if (lim != null) {
            Map<String, Integer> perType = new HashMap<>();
            ConfigurationSection types = lim.getConfigurationSection("per-type");
            if (types != null) {
                for (String id : types.getKeys(false)) {
                    perType.put(id, Math.max(0, types.getInt(id)));
                }
            }
            limits = new ChunkLimits(
                    Math.max(1, lim.getInt("blocks-per-chunk", base.blocksPerChunk())),
                    Math.max(1, lim.getInt("processors-per-chunk", base.processorsPerChunk())),
                    Math.max(1, lim.getInt("same-type-per-chunk", base.sameTypePerChunk())),
                    perType);
        }
        return new EngineConfig(budget, radius, delay, limits);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
