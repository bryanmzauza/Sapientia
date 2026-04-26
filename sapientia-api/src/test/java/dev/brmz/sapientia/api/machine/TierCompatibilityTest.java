package dev.brmz.sapientia.api.machine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TierCompatibilityTest {

    @Test
    void equalTiersAlwaysAllow() {
        for (MachineTier t : MachineTier.values()) {
            assertThat(TierCompatibility.check(t, t))
                    .isEqualTo(TierCompatibility.Policy.ALLOW);
            assertThat(TierCompatibility.isCompatible(t, t)).isTrue();
        }
    }

    @Test
    void overdrivingLowerTierBurnsCable() {
        // T-407 ADR-017: source > target → BURN
        assertThat(TierCompatibility.check(MachineTier.MV, MachineTier.LV))
                .isEqualTo(TierCompatibility.Policy.BURN);
        assertThat(TierCompatibility.check(MachineTier.HV, MachineTier.LV))
                .isEqualTo(TierCompatibility.Policy.BURN);
        assertThat(TierCompatibility.check(MachineTier.EV, MachineTier.MV))
                .isEqualTo(TierCompatibility.Policy.BURN);
    }

    @Test
    void underFeedingHigherTierClamps() {
        // source < target → silently clamp throughput
        assertThat(TierCompatibility.check(MachineTier.LV, MachineTier.MV))
                .isEqualTo(TierCompatibility.Policy.CLAMP);
        assertThat(TierCompatibility.check(MachineTier.MV, MachineTier.HV))
                .isEqualTo(TierCompatibility.Policy.CLAMP);
        assertThat(TierCompatibility.check(MachineTier.LV, MachineTier.EV))
                .isEqualTo(TierCompatibility.Policy.CLAMP);
    }

    @Test
    void mismatchedTiersAreNotCompatible() {
        assertThat(TierCompatibility.isCompatible(MachineTier.LV, MachineTier.MV)).isFalse();
        assertThat(TierCompatibility.isCompatible(MachineTier.MV, MachineTier.LV)).isFalse();
    }
}
