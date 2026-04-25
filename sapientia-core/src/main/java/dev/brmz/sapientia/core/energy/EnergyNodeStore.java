package dev.brmz.sapientia.core.energy;

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

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * SQLite-backed CRUD for {@code energy_nodes}. All operations run synchronously on
 * the caller thread; volume is low (one row per placed energy block) so a write-
 * behind queue is overkill for now. Write-behind can be folded in later behind the
 * same {@link dev.brmz.sapientia.core.persistence.WriteBehindQueue} pattern.
 */
public final class EnergyNodeStore {

    private final Logger logger;
    private final DataSource dataSource;

    public EnergyNodeStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void put(@NotNull SimpleEnergyNode node) {
        BlockKey k = node.location();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO energy_nodes (world, block_x, block_y, block_z, node_id, node_type, tier, buffer_curr, buffer_max, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET " +
                     "node_type = excluded.node_type, tier = excluded.tier, " +
                     "buffer_curr = excluded.buffer_curr, buffer_max = excluded.buffer_max, " +
                     "updated_at = excluded.updated_at")) {
            ps.setString(1, k.world());
            ps.setInt(2, k.x());
            ps.setInt(3, k.y());
            ps.setInt(4, k.z());
            ps.setString(5, node.nodeId().toString());
            ps.setString(6, node.type().name());
            ps.setString(7, node.tier().name());
            ps.setLong(8, node.bufferCurrent());
            ps.setLong(9, node.bufferMax());
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist energy node at " + k, e);
        }
    }

    public void delete(@NotNull BlockKey key) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM energy_nodes WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {
            ps.setString(1, key.world());
            ps.setInt(2, key.x());
            ps.setInt(3, key.y());
            ps.setInt(4, key.z());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete energy node at " + key, e);
        }
    }

    /** Loads every node stored for a given chunk (16×16, full Y range). */
    public @NotNull List<SimpleEnergyNode> loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        List<SimpleEnergyNode> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT block_x, block_y, block_z, node_id, node_type, tier, buffer_curr, buffer_max " +
                     "FROM energy_nodes WHERE world = ? AND block_x BETWEEN ? AND ? AND block_z BETWEEN ? AND ?")) {
            ps.setString(1, world);
            ps.setInt(2, xMin);
            ps.setInt(3, xMax);
            ps.setInt(4, zMin);
            ps.setInt(5, zMax);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BlockKey k = new BlockKey(world, rs.getInt(1), rs.getInt(2), rs.getInt(3));
                    UUID id = UUID.fromString(rs.getString(4));
                    EnergyNodeType type = EnergyNodeType.valueOf(rs.getString(5));
                    EnergyTier tier = EnergyTier.valueOf(rs.getString(6));
                    out.add(new SimpleEnergyNode(id, k, type, tier, rs.getLong(7), rs.getLong(8)));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load energy nodes for chunk " + world + " " + chunkX + "," + chunkZ, e);
        }
        return out;
    }
}
