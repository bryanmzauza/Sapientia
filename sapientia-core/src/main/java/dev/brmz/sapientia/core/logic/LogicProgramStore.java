package dev.brmz.sapientia.core.logic;

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
 * SQLite-backed persistence for {@link dev.brmz.sapientia.api.logic.LogicProgram}
 * sources (T-302 / 1.3.0). Programs are stored as canonical YAML so they can be
 * inspected on disk, exported via the command, and rehydrated at startup.
 */
public final class LogicProgramStore {

    private final Logger logger;
    private final DataSource dataSource;

    public LogicProgramStore(@NotNull Logger logger, @NotNull DataSource dataSource) {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public void upsert(@NotNull String name, @NotNull String yaml, boolean enabled, @Nullable String lastError) {
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO logic_programs (name, yaml_source, enabled, last_error, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(name) DO UPDATE SET
                    yaml_source = excluded.yaml_source,
                    enabled = excluded.enabled,
                    last_error = excluded.last_error,
                    updated_at = excluded.updated_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, name);
            st.setString(2, yaml);
            st.setInt(3, enabled ? 1 : 0);
            if (lastError == null) {
                st.setNull(4, java.sql.Types.VARCHAR);
            } else {
                st.setString(4, lastError);
            }
            st.setLong(5, now);
            st.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to persist logic program " + name, e);
        }
    }

    public void delete(@NotNull String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement("DELETE FROM logic_programs WHERE name = ?")) {
            st.setString(1, name);
            st.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete logic program " + name, e);
        }
    }

    public @NotNull Optional<StoredProgram> load(@NotNull String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "SELECT name, yaml_source, enabled, last_error FROM logic_programs WHERE name = ?")) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new StoredProgram(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getInt(3) != 0,
                            rs.getString(4)));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to load logic program " + name, e);
        }
        return Optional.empty();
    }

    public @NotNull List<StoredProgram> loadAll() {
        List<StoredProgram> out = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "SELECT name, yaml_source, enabled, last_error FROM logic_programs ORDER BY name");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                out.add(new StoredProgram(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getInt(3) != 0,
                        rs.getString(4)));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to enumerate logic programs", e);
        }
        return out;
    }

    public record StoredProgram(@NotNull String name,
                                @NotNull String yamlSource,
                                boolean enabled,
                                @Nullable String lastError) {
    }
}
