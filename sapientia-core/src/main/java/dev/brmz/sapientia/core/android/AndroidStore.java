package dev.brmz.sapientia.core.android;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.android.AndroidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQLite-backed persistence for android placements (T-451 / 1.9.0).
 *
 * <p>The kinetic AI behaviour ships in 1.9.1; until then the store only
 * persists placement metadata, the assigned program name and the four
 * upgrade tiers, so caps and program assignments survive restarts.
 *
 * <p>See V009__androids.sql.
 */
public final class AndroidStore {

    private final Logger logger;
    private final DataSource dataSource;

    public AndroidStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void upsert(@NotNull StoredAndroid row) {
        String sql = """
                INSERT INTO androids (world, block_x, block_y, block_z,
                        type, owner_uuid, program_name,
                        chip_tier, motor_tier, armour_tier, fuel_tier,
                        fuel_buffer, health, last_tick_ms)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET
                    type         = excluded.type,
                    owner_uuid   = excluded.owner_uuid,
                    program_name = excluded.program_name,
                    chip_tier    = excluded.chip_tier,
                    motor_tier   = excluded.motor_tier,
                    armour_tier  = excluded.armour_tier,
                    fuel_tier    = excluded.fuel_tier,
                    fuel_buffer  = excluded.fuel_buffer,
                    health       = excluded.health,
                    last_tick_ms = excluded.last_tick_ms
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, row.world());
            st.setInt(2, row.blockX());
            st.setInt(3, row.blockY());
            st.setInt(4, row.blockZ());
            st.setString(5, row.type().name());
            if (row.ownerUuid() == null) st.setNull(6, java.sql.Types.VARCHAR);
            else                          st.setString(6, row.ownerUuid().toString());
            if (row.programName() == null) st.setNull(7, java.sql.Types.VARCHAR);
            else                            st.setString(7, row.programName());
            st.setInt(8,  row.chipTier());
            st.setInt(9,  row.motorTier());
            st.setInt(10, row.armourTier());
            st.setInt(11, row.fuelTier());
            st.setLong(12, row.fuelBuffer());
            st.setInt(13, row.health());
            st.setLong(14, row.lastTickMs());
            st.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist android at "
                    + row.world() + " " + row.blockX() + "," + row.blockY() + "," + row.blockZ(), e);
        }
    }

    public void delete(@NotNull String world, int x, int y, int z) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "DELETE FROM androids WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {
            st.setString(1, world);
            st.setInt(2, x);
            st.setInt(3, y);
            st.setInt(4, z);
            st.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete android at " + world + " " + x + "," + y + "," + z, e);
        }
    }

    public @NotNull List<StoredAndroid> loadAll() {
        List<StoredAndroid> out = new ArrayList<>();
        String sql = """
                SELECT world, block_x, block_y, block_z, type, owner_uuid, program_name,
                       chip_tier, motor_tier, armour_tier, fuel_tier,
                       fuel_buffer, health, last_tick_ms
                FROM androids
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                AndroidType type;
                try {
                    type = AndroidType.valueOf(rs.getString(5));
                } catch (IllegalArgumentException e) {
                    logger.warning("Skipping android row with unknown type: " + rs.getString(5));
                    continue;
                }
                String ownerRaw = rs.getString(6);
                UUID owner = null;
                if (ownerRaw != null) {
                    try {
                        owner = UUID.fromString(ownerRaw);
                    } catch (IllegalArgumentException ignored) { /* keep null */ }
                }
                out.add(new StoredAndroid(
                        rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        type, owner, rs.getString(7),
                        rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11),
                        rs.getLong(12), rs.getInt(13), rs.getLong(14)));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to enumerate androids", e);
        }
        return out;
    }

    public record StoredAndroid(@NotNull String world,
                                int blockX, int blockY, int blockZ,
                                @NotNull AndroidType type,
                                @Nullable UUID ownerUuid,
                                @Nullable String programName,
                                int chipTier, int motorTier, int armourTier, int fuelTier,
                                long fuelBuffer, int health, long lastTickMs) {
    }
}
