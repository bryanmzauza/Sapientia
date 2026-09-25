package dev.brmz.sapientia.core.fluids;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.fluids.FluidStack;
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.persistence.NodeWriteQueue;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQLite persistence for {@code fluid_nodes}, including tank contents. Writes
 * go through a {@link NodeWriteQueue} flushed by the database thread.
 */
public final class FluidNodeStore {

    private final Logger logger;
    private final DataSource dataSource;

    private final NodeWriteQueue<Row> writes;

    public FluidNodeStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
        this.writes = new NodeWriteQueue<>(logger, dataSource, "fluid_nodes",
                "INSERT INTO fluid_nodes (world, block_x, block_y, block_z, chunk_x, chunk_z, node_id, node_type,"
                        + " tier, fluid_type, amount_mb, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET"
                        + " node_id = excluded.node_id, node_type = excluded.node_type, tier = excluded.tier,"
                        + " fluid_type = excluded.fluid_type, amount_mb = excluded.amount_mb,"
                        + " updated_at = excluded.updated_at",
                (ps, key, row) -> {
                    ps.setString(1, key.world());
                    ps.setInt(2, key.x());
                    ps.setInt(3, key.y());
                    ps.setInt(4, key.z());
                    ps.setInt(5, key.chunkX());
                    ps.setInt(6, key.chunkZ());
                    ps.setString(7, row.nodeId().toString());
                    ps.setString(8, row.type().name());
                    ps.setString(9, row.tier().name());
                    if (row.fluidId() == null) ps.setNull(10, java.sql.Types.VARCHAR);
                    else ps.setString(10, row.fluidId());
                    ps.setLong(11, row.amountMb());
                    ps.setLong(12, System.currentTimeMillis());
                },
                List.of("DELETE FROM fluid_nodes WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?"));
    }

    /** The queue the database thread flushes. */
    public @NotNull NodeWriteQueue<Row> writes() {
        return writes;
    }

    /** Queues an upsert of the node's row, copying its contents now. */
    public void put(@NotNull SimpleFluidNode node) {
        FluidStack contents = node.contents();
        writes.put(node.location(), new Row(node.nodeId(), node.type(), node.tier(),
                contents == null ? null : contents.type().id().toString(),
                contents == null ? 0L : contents.amountMb()));
    }

    /** Queues the deletion of the row at {@code key}. */
    public void delete(@NotNull BlockKey key) {
        writes.delete(key);
    }

    /** Tank contents copied on the main thread; the flush runs on the database thread. */
    public record Row(UUID nodeId, FluidNodeType type, EnergyTier tier, @Nullable String fluidId, long amountMb) {}

    /** Loads every node stored for a chunk. */
    public @NotNull List<SimpleFluidNode> loadChunk(
            @NotNull String world, int chunkX, int chunkZ,
            @NotNull Function<NamespacedKey, FluidType> typeLookup) {
        List<SimpleFluidNode> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT block_x, block_y, block_z, node_id, node_type, tier, fluid_type, amount_mb " +
                     "FROM fluid_nodes WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BlockKey k = new BlockKey(world, rs.getInt(1), rs.getInt(2), rs.getInt(3));
                    UUID id = UUID.fromString(rs.getString(4));
                    FluidNodeType type = FluidNodeType.valueOf(rs.getString(5));
                    EnergyTier tier = EnergyTier.valueOf(rs.getString(6));
                    String fluidIdRaw = rs.getString(7);
                    long amount = rs.getLong(8);
                    @Nullable FluidType heldType = null;
                    if (fluidIdRaw != null) {
                        NamespacedKey nk = NamespacedKey.fromString(fluidIdRaw);
                        if (nk != null) heldType = typeLookup.apply(nk);
                    }
                    out.add(new SimpleFluidNode(id, k, type, tier, heldType, amount));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load fluid nodes for chunk " + world + " " + chunkX + "," + chunkZ, e);
        }
        return out;
    }
}
