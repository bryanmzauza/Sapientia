package dev.brmz.sapientia.core.engine;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ChunkLimitsTest {

    private final ChunkLimits limits = new ChunkLimits(10, 4, 3, Map.of("sapientia:quarry_controller", 1));

    @Test
    void placementWithinAllLimitsIsAllowed() {
        assertThat(limits.check(new ChunkLimits.Counts(5, 2, 1), "sapientia:macerator", true, 0)).isNull();
    }

    @Test
    void totalBlocksLimitAppliesToEverything() {
        assertThat(limits.check(new ChunkLimits.Counts(10, 0, 0), "sapientia:cable", false, 0))
                .isEqualTo(new ChunkLimits.Violation(ChunkLimits.Kind.BLOCKS, 10));
    }

    @Test
    void processorLimitOnlyAppliesToProcessors() {
        ChunkLimits.Counts counts = new ChunkLimits.Counts(5, 4, 0);
        assertThat(limits.check(counts, "sapientia:cable", false, 0)).isNull();
        assertThat(limits.check(counts, "sapientia:macerator", true, 0))
                .isEqualTo(new ChunkLimits.Violation(ChunkLimits.Kind.PROCESSORS, 4));
    }

    @Test
    void sameTypeLimitPrefersConfigThenBlockThenDefault() {
        assertThat(limits.limitFor("sapientia:quarry_controller", 2)).isEqualTo(1);
        assertThat(limits.limitFor("sapientia:refinery", 2)).isEqualTo(2);
        assertThat(limits.limitFor("sapientia:macerator", 0)).isEqualTo(3);

        assertThat(limits.check(new ChunkLimits.Counts(1, 1, 1), "sapientia:quarry_controller", true, 0))
                .isEqualTo(new ChunkLimits.Violation(ChunkLimits.Kind.SAME_TYPE, 1));
    }
}
