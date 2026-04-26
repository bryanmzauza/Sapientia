package dev.brmz.sapientia.core.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class MigrationLoaderTest {

    private static final Logger LOGGER = Logger.getLogger(MigrationLoaderTest.class.getName());

    @Test
    void applyAllRunsBundledMigrationsInOrder() throws Exception {
        MigrationLoader loader = new MigrationLoader(LOGGER);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            loader.applyAll(connection);

            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT version, filename, checksum FROM schema_version ORDER BY version")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
                assertThat(rs.getString(2)).isEqualTo("V001__init.sql");
                assertThat(rs.getString(3)).isNotBlank();

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
                assertThat(rs.getString(2)).isEqualTo("V002__player_platform_cache.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(3);
                assertThat(rs.getString(2)).isEqualTo("V003__energy_nodes.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(4);
                assertThat(rs.getString(2)).isEqualTo("V004__unlocked_content.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(5);
                assertThat(rs.getString(2)).isEqualTo("V005__item_nodes.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(6);
                assertThat(rs.getString(2)).isEqualTo("V006__fluid_nodes.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(7);
                assertThat(rs.getString(2)).isEqualTo("V007__logic_programs.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(8);
                assertThat(rs.getString(2)).isEqualTo("V008__crude_oil_reservoirs.sql");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(9);
                assertThat(rs.getString(2)).isEqualTo("V009__androids.sql");

                assertThat(rs.next()).isFalse();
            }
        }
    }

    @Test
    void applyAllIsIdempotent() throws Exception {
        MigrationLoader loader = new MigrationLoader(LOGGER);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            loader.applyAll(connection);
            loader.applyAll(connection); // second invocation must be a no-op.

            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schema_version")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(9);
            }
        }
    }

    @Test
    void playerPlatformTableIsCreated() throws Exception {
        MigrationLoader loader = new MigrationLoader(LOGGER);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            loader.applyAll(connection);
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type='table' AND name='player_platform'")) {
                assertThat(rs.next()).isTrue();
            }
        }
    }
}
