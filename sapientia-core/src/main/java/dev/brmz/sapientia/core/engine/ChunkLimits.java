package dev.brmz.sapientia.core.engine;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-chunk placement limits. They stop extreme concentrations of Sapientia
 * blocks in one chunk, the main source of lag in technology plugins, while
 * staying far above what normal bases need.
 *
 * @param blocksPerChunk     every Sapientia block, including cables and pipes
 * @param processorsPerChunk blocks that run on the machine scheduler
 * @param sameTypePerChunk   default for any single block type
 * @param perType            overrides by block id (for example {@code sapientia:quarry_controller -> 1})
 */
public record ChunkLimits(int blocksPerChunk, int processorsPerChunk, int sameTypePerChunk,
                          @NotNull Map<String, Integer> perType) {

    public static final ChunkLimits DEFAULT = new ChunkLimits(2048, 256, 64, Map.of());

    /** Which limit a placement would break. */
    public enum Kind { BLOCKS, PROCESSORS, SAME_TYPE }

    /** A broken limit and its value. */
    public record Violation(@NotNull Kind kind, int limit) {}

    /** What is already in the chunk, not counting the block being placed. */
    public record Counts(int blocks, int processors, int sameType) {}

    public ChunkLimits {
        perType = Map.copyOf(perType);
    }

    /**
     * Returns the limit a placement would break, or {@code null} if it fits.
     *
     * @param blockId       id of the block being placed
     * @param processor     whether that block runs on the machine scheduler
     * @param declaredLimit the block's own limit ({@code <= 0} if it has none)
     */
    public @Nullable Violation check(@NotNull Counts counts, @NotNull String blockId,
                                     boolean processor, int declaredLimit) {
        if (counts.blocks() >= blocksPerChunk) {
            return new Violation(Kind.BLOCKS, blocksPerChunk);
        }
        if (processor && counts.processors() >= processorsPerChunk) {
            return new Violation(Kind.PROCESSORS, processorsPerChunk);
        }
        int typeLimit = limitFor(blockId, declaredLimit);
        if (counts.sameType() >= typeLimit) {
            return new Violation(Kind.SAME_TYPE, typeLimit);
        }
        return null;
    }

    /** Effective same-type limit: config override, then the block's own limit, then the default. */
    public int limitFor(@NotNull String blockId, int declaredLimit) {
        Integer override = perType.get(blockId);
        if (override != null) {
            return override;
        }
        return declaredLimit > 0 ? declaredLimit : sameTypePerChunk;
    }
}
