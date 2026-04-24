package dev.brmz.sapientia.core.block;

/** Immutable location key (world + block coords) used by the block store. */
public record BlockKey(String world, int x, int y, int z) {

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }
}
