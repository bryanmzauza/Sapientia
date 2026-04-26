package dev.brmz.sapientia.core.android;

import static org.assertj.core.api.Assertions.assertThat;

import dev.brmz.sapientia.api.android.AndroidType;
import org.junit.jupiter.api.Test;

/**
 * T-455 / 1.9.1 — loot table determinism + coverage guards.
 *
 * <p>The 1.9.1 contract from {@code docs/decision-log.md} (ADR-021) is that
 * loot is <em>simulated</em>, not generated from real entities. Tests here
 * lock both that contract (no spawn side-effects required to run) and the
 * "same seed → same roll" property the benchmark + behaviour engine depend
 * on for reproducibility.
 */
class AndroidLootTablesTest {

    @Test
    void lootRollIsDeterministicForFixedSeed() {
        for (AndroidType type : new AndroidType[]{
                AndroidType.FARMER, AndroidType.LUMBERJACK, AndroidType.MINER,
                AndroidType.FISHERMAN, AndroidType.BUTCHER, AndroidType.SLAYER}) {
            AndroidLootTables.LootDrop a = AndroidLootTables.roll(type, 0xCAFEBABEL);
            AndroidLootTables.LootDrop b = AndroidLootTables.roll(type, 0xCAFEBABEL);
            assertThat(a).isNotNull();
            assertThat(b).isNotNull();
            assertThat(a.material()).isEqualTo(b.material());
            assertThat(a.amount()).isEqualTo(b.amount());
        }
    }

    @Test
    void builderAndTraderHaveNoLootTable() {
        assertThat(AndroidLootTables.hasTable(AndroidType.BUILDER)).isFalse();
        assertThat(AndroidLootTables.hasTable(AndroidType.TRADER)).isFalse();
        assertThat(AndroidLootTables.roll(AndroidType.BUILDER, 1L)).isNull();
        assertThat(AndroidLootTables.roll(AndroidType.TRADER, 1L)).isNull();
    }

    @Test
    void allLootArchetypesAreRegistered() {
        assertThat(AndroidLootTables.tableCount()).isEqualTo(6);
    }

    @Test
    void distinctSeedsMostlyProduceDistinctRolls() {
        // Sanity check: the seed actually drives the picker (i.e. the
        // table isn't degenerate). Tolerant threshold to keep the test
        // resilient to future weight rebalances.
        int distinct = 0;
        AndroidLootTables.LootDrop last = null;
        for (long seed = 1; seed <= 64; seed++) {
            AndroidLootTables.LootDrop roll = AndroidLootTables.roll(AndroidType.MINER, seed);
            assertThat(roll).isNotNull();
            if (last == null || !roll.material().equals(last.material())) distinct++;
            last = roll;
        }
        assertThat(distinct).isGreaterThanOrEqualTo(8);
    }
}
