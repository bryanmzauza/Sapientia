package dev.brmz.sapientia.core.android;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T-456 / 1.9.0 — pure-POJO invariants for {@link AndroidConfig}. Mirrors
 * the {@code LogisticsConfigTest} pattern from 1.8.1.
 */
class AndroidConfigTest {

    @Test
    void defaultsMatchSpec() {
        AndroidConfig defaults = AndroidConfig.defaults();
        assertThat(defaults.serverCap()).isEqualTo(AndroidConfig.DEFAULT_SERVER_CAP);
        assertThat(AndroidConfig.DEFAULT_SERVER_CAP).isEqualTo(200);
    }

    @Test
    void clampsBelowMinimum() {
        // A negative or zero cap would deadlock placement entirely;
        // T-456 requires we clamp to MIN_SERVER_CAP (1).
        assertThat(new AndroidConfig(-50).serverCap()).isEqualTo(AndroidConfig.MIN_SERVER_CAP);
        assertThat(new AndroidConfig(0).serverCap()).isEqualTo(AndroidConfig.MIN_SERVER_CAP);
    }

    @Test
    void clampsAboveMaximum() {
        // Operator-friendly upper bound; far above what TPS supports
        // per the deferred P-020 benchmark.
        assertThat(new AndroidConfig(50_000).serverCap()).isEqualTo(AndroidConfig.MAX_SERVER_CAP);
        assertThat(AndroidConfig.MAX_SERVER_CAP).isEqualTo(5_000);
    }

    @Test
    void respectsValuesInRange() {
        assertThat(new AndroidConfig(1).serverCap()).isEqualTo(1);
        assertThat(new AndroidConfig(123).serverCap()).isEqualTo(123);
        assertThat(new AndroidConfig(5_000).serverCap()).isEqualTo(5_000);
    }
}
