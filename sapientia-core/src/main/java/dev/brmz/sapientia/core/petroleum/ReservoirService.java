package dev.brmz.sapientia.core.petroleum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;

/**
 * Per-chunk crude-oil reservoir bookkeeping (T-412 / 1.5.1).
 *
 * <p>Each chunk that is queried by a pumpjack is initialised once with a
 * deterministic random reserve in the range [{@value #MIN_INIT_MB},
 * {@value #MAX_INIT_MB}] mB. The reserve drains as pumpjacks pull fluid; it
 * regenerates very slowly ({@value #REGEN_MB_PER_MIN} mB/minute, capped at
 * {@code initial_mb}) so abandoned wells eventually refill but cannot sustain
 * a single pumpjack indefinitely (decision: ADR-018, finite slow-regen model).
 *
 * <p>Persistence: V008 schema. State is mirrored in-memory; writes flush
 * synchronously to keep the model robust against unclean shutdowns (the
 * dataset is tiny — one row per chunk per world).
 */
public final class ReservoirService {

    public static final int MIN_INIT_MB = 10_000;
    public static final int MAX_INIT_MB = 100_000;
    public static final int REGEN_MB_PER_MIN = 1;

    private final Logger logger;
    private final DataSource dataSource;
    private final Map<ChunkRef, Reservoir> cache = new ConcurrentHashMap<>();

    public ReservoirService(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    /** Loads (or initialises) the reservoir for a chunk. Never returns null. */
    public @NotNull Reservoir loadOrInit(@NotNull String world, int chunkX, int chunkZ) {
        ChunkRef ref = new ChunkRef(world, chunkX, chunkZ);
        Reservoir existing = cache.get(ref);
        if (existing != null) return existing;

        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT amount_mb, initial_mb, last_tick_ms FROM crude_oil_reservoirs "
                            + "WHERE world=? AND chunk_x=? AND chunk_z=?")) {
                ps.setString(1, world);
                ps.setInt(2, chunkX);
                ps.setInt(3, chunkZ);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Reservoir r = new Reservoir(ref, rs.getInt(1), rs.getInt(2), rs.getLong(3));
                        cache.put(ref, r);
                        return r;
                    }
                }
            }
            int initial = deterministicInitial(ref);
            long now = System.currentTimeMillis();
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO crude_oil_reservoirs(world, chunk_x, chunk_z, amount_mb, initial_mb, last_tick_ms) "
                            + "VALUES(?,?,?,?,?,?)")) {
                ins.setString(1, world);
                ins.setInt(2, chunkX);
                ins.setInt(3, chunkZ);
                ins.setInt(4, initial);
                ins.setInt(5, initial);
                ins.setLong(6, now);
                ins.executeUpdate();
            }
            Reservoir created = new Reservoir(ref, initial, initial, now);
            cache.put(ref, created);
            return created;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to load/init reservoir for " + ref, ex);
            return new Reservoir(ref, deterministicInitial(ref), MAX_INIT_MB, System.currentTimeMillis());
        }
    }

    /** Drains up to {@code request} mB; returns actually-drained mB. */
    public int drain(@NotNull String world, int chunkX, int chunkZ, int request) {
        if (request <= 0) return 0;
        Reservoir r = loadOrInit(world, chunkX, chunkZ);
        synchronized (r) {
            applyRegen(r);
            int give = Math.min(request, r.amountMb);
            r.amountMb -= give;
            persist(r);
            return give;
        }
    }

    /** Returns the current amount in mB (after regen). */
    public int amount(@NotNull String world, int chunkX, int chunkZ) {
        Reservoir r = loadOrInit(world, chunkX, chunkZ);
        synchronized (r) {
            applyRegen(r);
            return r.amountMb;
        }
    }

    private void applyRegen(Reservoir r) {
        long now = System.currentTimeMillis();
        long elapsedMin = (now - r.lastTickMs) / 60_000L;
        if (elapsedMin <= 0L) return;
        int add = (int) Math.min((long) Integer.MAX_VALUE, elapsedMin * REGEN_MB_PER_MIN);
        if (add <= 0) return;
        r.amountMb = (int) Math.min((long) r.initialMb, (long) r.amountMb + add);
        r.lastTickMs = now;
    }

    private void persist(Reservoir r) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE crude_oil_reservoirs SET amount_mb=?, last_tick_ms=? "
                             + "WHERE world=? AND chunk_x=? AND chunk_z=?")) {
            ps.setInt(1, r.amountMb);
            ps.setLong(2, r.lastTickMs);
            ps.setString(3, r.ref.world);
            ps.setInt(4, r.ref.chunkX);
            ps.setInt(5, r.ref.chunkZ);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to persist reservoir " + r.ref, ex);
        }
    }

    static int deterministicInitial(ChunkRef ref) {
        long h = 1469598103934665603L;
        for (int i = 0; i < ref.world.length(); i++) {
            h ^= ref.world.charAt(i);
            h *= 1099511628211L;
        }
        h ^= ref.chunkX * 0x9E3779B97F4A7C15L;
        h ^= ref.chunkZ * 0xBF58476D1CE4E5B9L;
        long range = (long) (MAX_INIT_MB - MIN_INIT_MB + 1);
        long pos = (h & 0x7FFFFFFFFFFFFFFFL) % range;
        return (int) (MIN_INIT_MB + pos);
    }

    public Map<ChunkRef, Reservoir> snapshot() {
        return new HashMap<>(cache);
    }

    public record ChunkRef(String world, int chunkX, int chunkZ) {}

    public static final class Reservoir {
        final ChunkRef ref;
        int amountMb;
        final int initialMb;
        long lastTickMs;
        Reservoir(ChunkRef ref, int amountMb, int initialMb, long lastTickMs) {
            this.ref = ref;
            this.amountMb = amountMb;
            this.initialMb = initialMb;
            this.lastTickMs = lastTickMs;
        }
        public int amountMb() { return amountMb; }
        public int initialMb() { return initialMb; }
        public ChunkRef ref() { return ref; }
    }
}
