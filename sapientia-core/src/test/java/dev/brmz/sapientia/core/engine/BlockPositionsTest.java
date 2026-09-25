package dev.brmz.sapientia.core.engine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

final class BlockPositionsTest {

    @ParameterizedTest
    @CsvSource({"0,0,0", "1,64,-1", "-30000000,-64,30000000", "33554431,2047,-33554432", "-17,319,42"})
    void blockPositionsRoundTrip(int x, int y, int z) {
        long packed = BlockPositions.pack(x, y, z);
        assertThat(BlockPositions.x(packed)).isEqualTo(x);
        assertThat(BlockPositions.y(packed)).isEqualTo(y);
        assertThat(BlockPositions.z(packed)).isEqualTo(z);
        assertThat(BlockPositions.chunkOf(packed)).isEqualTo(BlockPositions.chunk(x >> 4, z >> 4));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "-1,1", "1875000,-1875000", "-2147483648,2147483647"})
    void chunkPositionsRoundTrip(int x, int z) {
        long packed = BlockPositions.chunk(x, z);
        assertThat(BlockPositions.chunkX(packed)).isEqualTo(x);
        assertThat(BlockPositions.chunkZ(packed)).isEqualTo(z);
    }
}
