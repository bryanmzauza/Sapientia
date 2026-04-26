package dev.brmz.sapientia.core.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T-444 / 1.8.1 — pure-POJO invariants for {@link LogisticsConfig}. Tests run
 * without Bukkit because parsing falls back to defaults on null input.
 */
class LogisticsConfigTest {

    @Test
    void defaultIsLegacy() {
        assertThat(LogisticsConfig.defaults().solver())
                .isEqualTo(LogisticsConfig.Solver.LEGACY);
    }

    @Test
    void parsesMaxflowAlias() {
        assertThat(LogisticsConfig.Solver.parse("maxflow"))
                .isEqualTo(LogisticsConfig.Solver.MAXFLOW);
        assertThat(LogisticsConfig.Solver.parse("MAX-FLOW"))
                .isEqualTo(LogisticsConfig.Solver.MAXFLOW);
        assertThat(LogisticsConfig.Solver.parse("max_flow"))
                .isEqualTo(LogisticsConfig.Solver.MAXFLOW);
    }

    @Test
    void parsesLegacyExplicitly() {
        assertThat(LogisticsConfig.Solver.parse("legacy"))
                .isEqualTo(LogisticsConfig.Solver.LEGACY);
    }

    @Test
    void unknownValuesFallBackToLegacy() {
        // Critical safety: a typo in config.yml must never break routing.
        assertThat(LogisticsConfig.Solver.parse("greedy"))
                .isEqualTo(LogisticsConfig.Solver.LEGACY);
        assertThat(LogisticsConfig.Solver.parse(""))
                .isEqualTo(LogisticsConfig.Solver.LEGACY);
        assertThat(LogisticsConfig.Solver.parse(null))
                .isEqualTo(LogisticsConfig.Solver.LEGACY);
    }
}
