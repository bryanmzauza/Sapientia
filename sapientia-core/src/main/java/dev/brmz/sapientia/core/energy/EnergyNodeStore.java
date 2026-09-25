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
import dev.brmz.sapientia.core.persistence.NodeWriteQueue;
import org.jetbrains.annotations.NotNull;

/**
 * SQLite persistence for {@code energy_nodes}. Writes go through a
 * {@link NodeWriteQueue} flushed by the database thread; {@link #loadChunk}
 * runs on the caller's thread (the database thread during chunk loads).
 */
public final class EnergyNodeStore {

    private final Logger logger;
    private final DataSource dataSource;
    private final NodeWriteQueue<SimpleEnergyNode> writes;

    public EnergyNodeStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
        this.writes = new NodeWriteQueue<>(logger, dataSource, "energy_nodes",
                "INSERT INTO energy_nodes (world, block_x, block_y, block_z, chunk_x, chunk_z, node_id, node_type,"
                        + " tier, buffer_curr, buffer_max, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET"
                        + " node_id = excluded.node_id, node_type = excluded.node_type, tier = excluded.tier,"
                        + " buffer_curr = excluded.buffer_curr, buffer_max = excluded.buffer_max,"
                        + " updated_at = excluded.updated_at",
                (ps, key, node) -> {
                    ps.setString(1, key.world());
                    ps.setInt(2, key.x());
                    ps.setInt(3, key.y());
                    ps.setInt(4, key.z());
                    ps.setInt(5, key.chunkX());
                    ps.setInt(6, key.chunkZ());
                    ps.setString(7, node.nodeId().toString());
                    ps.setString(8, node.type().name());
                    ps.setString(9, node.tier().name());
                    // The buffer is atomic: the flush stores its latest value.
                    ps.setLong(10, node.bufferCurrent());
                    ps.setLong(11, node.bufferMax());
                    ps.setLong(12, System.currentTimeMillis());
                },
                List.of("DELETE FROM energy_nodes WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?"));
    }

    /** The queue the database thread flushes. */
    public @NotNull NodeWriteQueue<SimpleEnergyNode> writes() {
        return writes;
    }

    /** Queues an upsert of the node's row. */
    public void put(@NotNull SimpleEnergyNode node) {
        writes.put(node.location(), node);
    }

    /** Queues the deletion of the row at {@code key}. */
    public void delete(@NotNull BlockKey key) {
        writes.delete(key);
    }

    /** Loads every node stored for a chunk. */
    public @NotNull List<SimpleEnergyNode> loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        List<SimpleEnergyNode> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT block_x, block_y, block_z, node_id, node_type, tier, buffer_curr, buffer_max "
                             + "FROM energy_nodes WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
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
