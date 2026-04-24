package dev.brmz.sapientia.core.block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CRUD access to the {@code custom_blocks} table. Used by the block registry to
 * persist Sapientia-placed blocks across restarts. See docs/persistence-schema.md §3.
 */
public final class CustomBlockStore {

    private final Logger logger;
    private final DataSource dataSource;

    public CustomBlockStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void put(@NotNull BlockKey key, @NotNull String itemId, @Nullable byte[] stateBlob) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO custom_blocks(world, chunk_x, chunk_z, block_x, block_y, block_z,"
                             + " item_id, state_blob, updated_at)"
                             + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                             + " ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET"
                             + " item_id = excluded.item_id,"
                             + " state_blob = excluded.state_blob,"
                             + " updated_at = excluded.updated_at")) {
            ps.setString(1, key.world());
            ps.setInt(2, key.chunkX());
            ps.setInt(3, key.chunkZ());
            ps.setInt(4, key.x());
            ps.setInt(5, key.y());
            ps.setInt(6, key.z());
            ps.setString(7, itemId);
            if (stateBlob == null) {
                ps.setNull(8, java.sql.Types.BLOB);
            } else {
                ps.setBytes(8, stateBlob);
            }
            ps.setLong(9, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist custom block at " + key, e);
        }
    }

    public @NotNull Optional<StoredBlock> get(@NotNull BlockKey key) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT item_id, state_blob FROM custom_blocks"
                             + " WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {
            ps.setString(1, key.world());
            ps.setInt(2, key.x());
            ps.setInt(3, key.y());
            ps.setInt(4, key.z());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new StoredBlock(rs.getString(1), rs.getBytes(2)));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read custom block at " + key, e);
        }
        return Optional.empty();
    }

    public void remove(@NotNull BlockKey key) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "DELETE FROM custom_blocks WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {
            ps.setString(1, key.world());
            ps.setInt(2, key.x());
            ps.setInt(3, key.y());
            ps.setInt(4, key.z());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete custom block at " + key, e);
        }
    }

    public @NotNull List<StoredBlock> loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        List<StoredBlock> out = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT item_id, state_blob FROM custom_blocks"
                             + " WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new StoredBlock(rs.getString(1), rs.getBytes(2)));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING,
                    "Failed to load chunk (" + chunkX + "," + chunkZ + ") in " + world, e);
        }
        return out;
    }

    public record StoredBlock(String itemId, byte[] stateBlob) {}
}
