package dev.brmz.sapientia.core.android;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T-454 / 1.9.1 — guards the upgrade tier scaling table. Any rebalance
 * must roll through here so addons that depend on
 * {@link AndroidUpgradeScaling} get a clean diff in PR review.
 */
class AndroidUpgradeScalingTest {

    @Test
    void chipScanRadiusFollowsTierTable() {
        assertThat(AndroidUpgradeScaling.chipScanRadius(1)).isEqualTo(4);
        assertThat(AndroidUpgradeScaling.chipScanRadius(2)).isEqualTo(6);
        assertThat(AndroidUpgradeScaling.chipScanRadius(3)).isEqualTo(9);
        assertThat(AndroidUpgradeScaling.chipScanRadius(4)).isEqualTo(13);
    }

    @Test
    void motorCooldownShortensWithTier() {
        assertThat(AndroidUpgradeScaling.motorCooldownTicks(1)).isEqualTo(20);
        assertThat(AndroidUpgradeScaling.motorCooldownTicks(2)).isEqualTo(14);
        assertThat(AndroidUpgradeScaling.motorCooldownTicks(3)).isEqualTo(9);
        assertThat(AndroidUpgradeScaling.motorCooldownTicks(4)).isEqualTo(5);
    }

    @Test
    void armourRaisesHpAndAddsDamageReduction() {
        assertThat(AndroidUpgradeScaling.armourMaxHp(1)).isEqualTo(100);
        assertThat(AndroidUpgradeScaling.armourMaxHp(4)).isEqualTo(800);
        assertThat(AndroidUpgradeScaling.armourDamageReduction(1)).isZero();
        assertThat(AndroidUpgradeScaling.armourDamageReduction(4)).isEqualTo(4);
    }

    @Test
    void fuelBufferGrowsByPowersOfFour() {
        assertThat(AndroidUpgradeScaling.fuelBufferMax(1)).isEqualTo(1_000L);
        assertThat(AndroidUpgradeScaling.fuelBufferMax(2)).isEqualTo(4_000L);
        assertThat(AndroidUpgradeScaling.fuelBufferMax(3)).isEqualTo(16_000L);
        assertThat(AndroidUpgradeScaling.fuelBufferMax(4)).isEqualTo(64_000L);
    }

    @Test
    void biofuelRatioIsLockedAtOneSuPer100Mb() {
        assertThat(AndroidUpgradeScaling.BIOFUEL_SU_RATIO).isEqualTo(100L);
        assertThat(AndroidUpgradeScaling.BIOFUEL_PER_INSTRUCTION).isEqualTo(10L);
    }

    @Test
    void outOfRangeTiersFallBackToTierOne() {
        assertThat(AndroidUpgradeScaling.chipScanRadius(0)).isEqualTo(4);
        assertThat(AndroidUpgradeScaling.chipScanRadius(99)).isEqualTo(13);
        assertThat(AndroidUpgradeScaling.motorCooldownTicks(-3)).isEqualTo(20);
    }
}
