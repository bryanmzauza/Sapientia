package dev.brmz.sapientia.core.mining;

/** A 16×16×16 chunk section as 4096 bits in 64 longs. */
final class SectionBits {

    static final int LONGS = 64;

    private SectionBits() {}

    /** Bit index of a block inside its section. */
    static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    static boolean get(long[] bits, int index) {
        return (bits[index >>> 6] & (1L << (index & 63))) != 0;
    }

    static void set(long[] bits, int index, boolean value) {
        if (value) {
            bits[index >>> 6] |= 1L << (index & 63);
        } else {
            bits[index >>> 6] &= ~(1L << (index & 63));
        }
    }
}
