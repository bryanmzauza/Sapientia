package dev.brmz.sapientia.api.android;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Public android service (T-451 / 1.9.0). Mirrors the
 * {@link dev.brmz.sapientia.api.energy.EnergyService} /
 * {@link dev.brmz.sapientia.api.logistics.ItemService} surface: register a
 * node when a block is placed, unregister on break, query for live state.
 *
 * <p>1.9.0 ships the catalogue + persistence + caps + event scaffolding. The
 * kinetic AI loop (loot simulation, crop / log / ore scans, slayer melee,
 * trader exchanges) lands in 1.9.1.
 */
public interface AndroidService {

    /**
     * Adds a node for the given placed block. Returns {@code false} when the
     * placement would exceed the per-chunk cap (4) or the configured
     * server-wide cap (default 200) — in that case the caller must roll the
     * placement back. See T-456 / {@code AndroidCaps}.
     */
    boolean addNode(@NotNull Block block, @NotNull AndroidType type, UUID ownerUuid);

    /** Removes the node at the given block (no-op when there is none). */
    void removeNode(@NotNull Block block);

    /** Returns a snapshot of the node at the given block, if any. */
    @NotNull Optional<AndroidNode> nodeAt(@NotNull Block block);

    /**
     * Snapshot of every android currently loaded. Order is unspecified.
     * Used by {@code AndroidTicker} (1.9.0 placeholder, 1.9.1 kinetic loop).
     */
    @NotNull Collection<AndroidNode> all();

    /**
     * Number of androids currently loaded in the given chunk. Used by the
     * cap enforcement (T-456).
     */
    int countInChunk(@NotNull String world, int chunkX, int chunkZ);

    /** Total number of androids currently registered server-wide. */
    int totalCount();

    /**
     * Assigns a logic program by name to this android. The program must
     * already exist in {@link dev.brmz.sapientia.api.logic.LogicService}; the
     * service is responsible for cycle detection (T-302). Returns
     * {@code false} when no android lives at that block.
     */
    boolean assignProgram(@NotNull Block block, @NotNull String programName);

    /** Clears any assigned program. Returns {@code false} when no android lives at the block. */
    boolean clearProgram(@NotNull Block block);

    /**
     * Installs the given upgrade in the matching slot. Higher tiers replace
     * lower ones in the same slot (kind). Returns {@code false} when no
     * android lives at the block.
     */
    boolean setUpgrade(@NotNull Block block, @NotNull AndroidUpgrade upgrade);

    /** Hard cap of androids per chunk (T-456). Constant: 4. */
    int chunkCap();

    /** Configurable server-wide cap (T-456). Default 200. */
    int serverCap();
}
