package dev.brmz.sapientia.core.engine;

import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/** Answers whether blocks in a chunk may do work (see {@link ActivityMap}). */
@FunctionalInterface
public interface ActivityFilter {

    /** Everything active; used when no tracker is wired (tests, benchmarks). */
    ActivityFilter ALWAYS = (world, chunkX, chunkZ) -> true;

    boolean isActive(@NotNull String world, int chunkX, int chunkZ);

    default boolean isActive(@NotNull BlockKey key) {
        return isActive(key.world(), key.chunkX(), key.chunkZ());
    }
}
