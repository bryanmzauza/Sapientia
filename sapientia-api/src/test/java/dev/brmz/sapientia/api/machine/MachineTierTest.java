package dev.brmz.sapientia.api.machine;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.junit.jupiter.api.Test;

class MachineTierTest {

    @Test
    void mapsToEnergyTier() {
        assertThat(MachineTier.LV.energyTier()).isEqualTo(EnergyTier.LOW);
        assertThat(MachineTier.MV.energyTier()).isEqualTo(EnergyTier.MID);
        assertThat(MachineTier.HV.energyTier()).isEqualTo(EnergyTier.HIGH);
        assertThat(MachineTier.EV.energyTier()).isEqualTo(EnergyTier.EXTREME);
    }

    @Test
    void throughputScalesByTier() {
        assertThat(MachineTier.LV.maxThroughput()).isEqualTo(32L);
        assertThat(MachineTier.MV.maxThroughput()).isEqualTo(128L);
        assertThat(MachineTier.HV.maxThroughput()).isEqualTo(512L);
        assertThat(MachineTier.EV.maxThroughput()).isEqualTo(2048L);
    }

    @Test
    void roundTripsFromEnergyTier() {
        for (EnergyTier tier : EnergyTier.values()) {
            assertThat(MachineTier.fromEnergyTier(tier).energyTier()).isEqualTo(tier);
        }
    }
}
