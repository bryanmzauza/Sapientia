package dev.brmz.sapientia.core.android;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T-451 / 1.9.0 — instructions-per-tick contract guard. The full DAG
 * execution loop ships in 1.9.1, but the per-tick budget is locked here so
 * future patches cannot silently exceed it.
 */
class AndroidTickerTest {

    @Test
    void instructionsPerTickCapIsOne() {
        assertThat(AndroidTicker.INSTRUCTIONS_PER_TICK_CAP).isEqualTo(1);
    }
}
