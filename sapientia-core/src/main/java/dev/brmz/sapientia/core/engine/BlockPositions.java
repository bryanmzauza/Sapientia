package dev.brmz.sapientia.core.engine;

import org.jetbrains.annotations.NotNull;

/**
 * Packs block and chunk coordinates into primitive longs, so hot data
 * structures avoid per-position objects.
 *
 * <p>Block layout matches Minecraft's: 26 bits of x, 26 bits of z and 12 bits
 * of y (covers ±33 million horizontally and −2048..2047 vertically).
 */
public final class BlockPositions {

    private BlockPositions() {}

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    public static int x(long packed) {
        return (int) (packed >> 38);
    }

    public static int y(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public static int z(long packed) {
        return (int) (packed << 26 >> 38);
    }

    /** Packs chunk coordinates into one long. */
    public static long chunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int chunkX(long packedChunk) {
        return (int) (packedChunk >> 32);
    }

    public static int chunkZ(long packedChunk) {
        return (int) packedChunk;
    }

    /** Chunk of a packed block position. */
    public static long chunkOf(long packedBlock) {
        return chunk(x(packedBlock) >> 4, z(packedBlock) >> 4);
    }

    public static @NotNull String describe(long packed) {
        return x(packed) + "," + y(packed) + "," + z(packed);
    }
}
