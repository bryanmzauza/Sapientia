package dev.brmz.sapientia.core.android;

/**
 * Hard constants for android cap enforcement (T-456 / 1.9.0).
 *
 * <p>The per-chunk cap is locked at 4 by design — the hard cap exists so
 * naive farms can't paper over an entire chunk with butchers. The
 * server-wide cap is configurable via {@link AndroidConfig#serverCap()}
 * (default 200) but enforced through the same code path.
 *
 * <p>Both caps are checked at placement time; a denied placement
 * cancels the underlying {@code SapientiaBlockPlaceEvent} so vanilla
 * Bukkit rolls the block back automatically.
 */
public final class AndroidCaps {

    /** Hard, non-configurable cap — every loaded chunk holds at most 4 androids. */
    public static final int CHUNK_CAP = 4;

    private AndroidCaps() {}
}
