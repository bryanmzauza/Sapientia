package dev.brmz.sapientia.core.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

/**
 * Owns the SQLite connection pool and kicks off migrations on startup.
 * See docs/persistence-schema.md §1.
 */
public final class DatabaseManager implements AutoCloseable {

    private final Logger logger;
    private final Path dbFile;
    private HikariDataSource dataSource;

    public DatabaseManager(@NotNull Logger logger, @NotNull Path dataDirectory) {
        this.logger = logger;
        this.dbFile = dataDirectory.resolve("sapientia.db");
    }

    public void start(int poolSize) throws SQLException {
        try {
            Files.createDirectories(dbFile.getParent());
        } catch (IOException e) {
            throw new SQLException("Cannot create data directory " + dbFile.getParent(), e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath()
                + "?journal_mode=WAL&synchronous=NORMAL&foreign_keys=on");
        config.setPoolName("sapientia-sqlite");
        config.setMaximumPoolSize(Math.max(1, poolSize));
        // SQLite serialises writes; keep a small pool but allow concurrent readers.
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            new MigrationLoader(logger).applyAll(connection);
        }
        logger.info(() -> "SQLite ready at " + dbFile.toAbsolutePath());
    }

    public @NotNull DataSource dataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager has not been started.");
        }
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
