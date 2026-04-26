package dev.brmz.sapientia.core.logistics;

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

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.logistics.ItemFilterMode;
import dev.brmz.sapientia.api.logistics.ItemFilterRule;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.core.block.BlockKey;
import org.jetbrains.annotations.NotNull;

/**
 * SQLite-backed CRUD for {@code item_nodes} + {@code item_filter_rules}
 * (T-300 / 1.1.0). Mirrors {@code EnergyNodeStore}: synchronous, low volume,
 * fine-grained per-node updates. Filter rules are written as a full replace
 * to keep the index list compact.
 */
public final class ItemNodeStore {

    private final Logger logger;
    private final DataSource dataSource;

    public ItemNodeStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void put(@NotNull SimpleItemNode node) {
        BlockKey k = node.location();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO item_nodes (world, block_x, block_y, block_z, node_id, node_type, tier, priority, routing_policy, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?) " +
                     "ON CONFLICT(world, block_x, block_y, block_z) DO UPDATE SET " +
                     "node_type = excluded.node_type, tier = excluded.tier, " +
                     "priority = excluded.priority, updated_at = excluded.updated_at")) {
            ps.setString(1, k.world());
            ps.setInt(2, k.x());
            ps.setInt(3, k.y());
            ps.setInt(4, k.z());
            ps.setString(5, node.nodeId().toString());
            ps.setString(6, node.type().name());
            ps.setString(7, node.tier().name());
            ps.setInt(8, node.priority());
            ps.setLong(9, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist item node at " + k, e);
        }
    }

    public void delete(@NotNull BlockKey key) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement nodeDel = c.prepareStatement(
                     "DELETE FROM item_nodes WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ? RETURNING node_id")) {
            nodeDel.setString(1, key.world());
            nodeDel.setInt(2, key.x());
            nodeDel.setInt(3, key.y());
            nodeDel.setInt(4, key.z());
            try (ResultSet rs = nodeDel.executeQuery()) {
                if (rs.next()) {
                    String nodeId = rs.getString(1);
                    try (PreparedStatement rulesDel = c.prepareStatement(
                            "DELETE FROM item_filter_rules WHERE filter_node_id = ?")) {
                        rulesDel.setString(1, nodeId);
                        rulesDel.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete item node at " + key, e);
        }
    }

    public @NotNull List<SimpleItemNode> loadChunk(@NotNull String world, int chunkX, int chunkZ) {
        int xMin = chunkX * 16, xMax = xMin + 15;
        int zMin = chunkZ * 16, zMax = zMin + 15;
        List<SimpleItemNode> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT block_x, block_y, block_z, node_id, node_type, tier, priority " +
                     "FROM item_nodes WHERE world = ? AND block_x BETWEEN ? AND ? AND block_z BETWEEN ? AND ?")) {
            ps.setString(1, world);
            ps.setInt(2, xMin);
            ps.setInt(3, xMax);
            ps.setInt(4, zMin);
            ps.setInt(5, zMax);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BlockKey k = new BlockKey(world, rs.getInt(1), rs.getInt(2), rs.getInt(3));
                    UUID id = UUID.fromString(rs.getString(4));
                    ItemNodeType type = ItemNodeType.valueOf(rs.getString(5));
                    EnergyTier tier = EnergyTier.valueOf(rs.getString(6));
                    int priority = rs.getInt(7);
                    out.add(new SimpleItemNode(id, k, type, tier, priority));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load item nodes for chunk " + world + " " + chunkX + "," + chunkZ, e);
        }
        return out;
    }

    /** Replaces the rule list for a filter node atomically. */
    public void replaceFilterRules(@NotNull UUID filterNodeId, @NotNull List<ItemFilterRule> rules) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM item_filter_rules WHERE filter_node_id = ?")) {
                del.setString(1, filterNodeId.toString());
                del.executeUpdate();
            }
            if (!rules.isEmpty()) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO item_filter_rules (filter_node_id, rule_index, mode, pattern, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
                    long now = System.currentTimeMillis();
                    for (ItemFilterRule rule : rules) {
                        ins.setString(1, filterNodeId.toString());
                        ins.setInt(2, rule.index());
                        ins.setString(3, rule.mode().name());
                        ins.setString(4, rule.pattern());
                        ins.setLong(5, now);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to write filter rules for node " + filterNodeId, e);
        }
    }

    public @NotNull List<ItemFilterRule> loadFilterRules(@NotNull UUID filterNodeId) {
        List<ItemFilterRule> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT rule_index, mode, pattern FROM item_filter_rules " +
                     "WHERE filter_node_id = ? ORDER BY rule_index ASC")) {
            ps.setString(1, filterNodeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ItemFilterRule(
                            rs.getInt(1),
                            ItemFilterMode.valueOf(rs.getString(2)),
                            rs.getString(3)));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load filter rules for node " + filterNodeId, e);
        }
        return out;
    }
}
