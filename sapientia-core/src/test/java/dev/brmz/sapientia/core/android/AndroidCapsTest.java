package dev.brmz.sapientia.core.android;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T-456 / 1.9.0 — guard the chunk-cap invariant. Surfaced as a constant so
 * any change requires a roadmap revision; this test guarantees the value
 * does not silently drift.
 */
class AndroidCapsTest {

    @Test
    void chunkCapIsFour() {
        // Locked by the 1.9.0 spec; do not relax without an ADR update.
        assertThat(AndroidCaps.CHUNK_CAP).isEqualTo(4);
    }
}
