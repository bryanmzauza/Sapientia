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
import dev.brmz.sapientia.api.fluids.FluidType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQLite-backed CRUD for {@code fluid_nodes} (T-301 / 1.2.0). Mirrors
 * {@code ItemNodeStore} but persists per-tank fluid contents.
 */
public final class FluidNodeStore {

    private final Logger logger;
    private final DataSource dataSource;

    public FluidNodeStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void put(@NotNull SimpleFluidNode node) {
        BlockKey k = node.location();
        var contents = node.contents();
        String fluidId = contents == null ? null : contents.type().id().toString();
        long amount = contents == null ? 0L : contents.amountMb();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO fluid_nodes (world, block_x, block_y, block_z, node_id, node_type, tier, fluid_type, amount_mb, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET " +
                     "node_type = excluded.node_type, tier = excluded.tier, " +
                     "fluid_type = excluded.fluid_type, amount_mb = excluded.amount_mb, " +
                     "updated_at = excluded.updated_at")) {
            ps.setString(1, k.world());
            ps.setInt(2, k.x());
            ps.setInt(3, k.y());
            ps.setInt(4, k.z());
            ps.setString(5, node.nodeId().toString());
            ps.setString(6, node.type().name());
            ps.setString(7, node.tier().name());
            if (fluidId == null) ps.setNull(8, java.sql.Types.VARCHAR);
            else ps.setString(8, fluidId);
            ps.setLong(9, amount);
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist fluid node at " + k, e);
        }
    }

    public void delete(@NotNull BlockKey key) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM fluid_nodes WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?")) {
            ps.setString(1, key.world());
            ps.setInt(2, key.x());
            ps.setInt(3, key.y());
            ps.setInt(4, key.z());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete fluid node at " + key, e);
        }
    }

    public @NotNull List<SimpleFluidNode> loadChunk(
            @NotNull String world, int chunkX, int chunkZ,
            @NotNull Function<NamespacedKey, FluidType> typeLookup) {
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        List<SimpleFluidNode> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT block_x, block_y, block_z, node_id, node_type, tier, fluid_type, amount_mb " +
                     "FROM fluid_nodes WHERE world = ? AND block_x BETWEEN ? AND ? AND block_z BETWEEN ? AND ?")) {
            ps.setString(1, world);
            ps.setInt(2, xMin);
            ps.setInt(3, xMax);
            ps.setInt(4, zMin);
            ps.setInt(5, zMax);
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
