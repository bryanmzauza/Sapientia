package dev.brmz.sapientia.core.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

/**
 * Applies numbered SQL migrations from {@code classpath:db/migrations/VNNN__*.sql}.
 * Tracks state in {@code schema_version} and aborts when a checksum mismatch is
 * detected. See docs/persistence-schema.md §2.
 */
public final class MigrationLoader {

    private static final Pattern FILE_PATTERN = Pattern.compile("V(\\d+)__([a-zA-Z0-9_]+)\\.sql");
    private static final String CLASSPATH_DIR = "db/migrations";

    private final Logger logger;

    public MigrationLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void applyAll(@NotNull Connection connection) throws SQLException {
        ensureSchemaVersionTable(connection);
        Map<Integer, AppliedMigration> applied = loadApplied(connection);
        List<Migration> migrations = discoverMigrations();
        migrations.sort((a, b) -> Integer.compare(a.version, b.version));

        for (Migration migration : migrations) {
            AppliedMigration prior = applied.get(migration.version);
            if (prior != null) {
                if (!prior.checksum.equals(migration.checksum)) {
                    throw new SQLException(
                            "Migration V" + migration.version + " (" + migration.filename + ") "
                                    + "has been modified after being applied. Expected checksum "
                                    + prior.checksum + " but found " + migration.checksum + ".");
                }
                continue;
            }
            applySingle(connection, migration);
        }
    }

    private void applySingle(Connection connection, Migration migration) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            for (String stmt : splitStatements(migration.sql)) {
                if (!stmt.isBlank()) {
                    statement.execute(stmt);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO schema_version(version, filename, applied_at, checksum) "
                            + "VALUES (?, ?, ?, ?)")) {
                insert.setInt(1, migration.version);
                insert.setString(2, migration.filename);
                insert.setLong(3, System.currentTimeMillis());
                insert.setString(4, migration.checksum);
                insert.executeUpdate();
            }
            connection.commit();
            logger.info(() -> "Applied migration V" + migration.version + " " + migration.filename);
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Failed to apply migration " + migration.filename, e);
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void ensureSchemaVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS schema_version ("
                            + "version INTEGER PRIMARY KEY,"
                            + "filename TEXT NOT NULL,"
                            + "applied_at INTEGER NOT NULL,"
                            + "checksum TEXT NOT NULL)");
        }
    }

    private Map<Integer, AppliedMigration> loadApplied(Connection connection) throws SQLException {
        Map<Integer, AppliedMigration> result = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT version, filename, checksum FROM schema_version")) {
            while (rs.next()) {
                result.put(rs.getInt(1), new AppliedMigration(
                        rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
        }
        return result;
    }

    private List<Migration> discoverMigrations() throws SQLException {
        List<Migration> list = new ArrayList<>();
        try {
            for (Path path : listMigrationFiles()) {
                Matcher m = FILE_PATTERN.matcher(path.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                byte[] bytes = Files.readAllBytes(path);
                String sql = new String(bytes, StandardCharsets.UTF_8);
                list.add(new Migration(
                        Integer.parseInt(m.group(1)),
                        path.getFileName().toString(),
                        sql,
                        sha256(bytes)));
            }
        } catch (IOException | URISyntaxException e) {
            throw new SQLException("Failed to enumerate migrations", e);
        }
        return list;
    }

    private List<Path> listMigrationFiles() throws IOException, URISyntaxException {
        List<Path> paths = new ArrayList<>();
        java.net.URL dirUrl = getClass().getClassLoader().getResource(CLASSPATH_DIR);
        if (dirUrl == null) {
            // No migrations directory on classpath — fall back to streaming known files.
            return readFromStreamIndex();
        }
        URI uri = dirUrl.toURI();
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = openFileSystem(uri)) {
                Path dir = fs.getPath("/" + CLASSPATH_DIR);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.sql")) {
                    for (Path p : stream) {
                        // Copy bytes out of the FS into a heap path for later reading.
                        Path tmp = Files.createTempFile("sapientia-mig-", ".sql");
                        Files.copy(p, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        tmp.toFile().deleteOnExit();
                        Path renamed = tmp.resolveSibling(p.getFileName().toString());
                        Files.move(tmp, renamed, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        renamed.toFile().deleteOnExit();
                        paths.add(renamed);
                    }
                }
            }
        } else {
            Path dir = Paths.get(uri);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.sql")) {
                for (Path p : stream) {
                    paths.add(p);
                }
            }
        }
        return paths;
    }

    private FileSystem openFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (Exception ignored) {
            return FileSystems.newFileSystem(uri, Collections.emptyMap());
        }
    }

    /** Fallback: stream files declared in {@code db/migrations/index.txt} if present. */
    private List<Path> readFromStreamIndex() throws IOException {
        List<Path> out = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(CLASSPATH_DIR + "/index.txt")) {
            if (in == null) {
                return out;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String name = line.trim();
                    if (name.isEmpty() || name.startsWith("#")) {
                        continue;
                    }
                    try (InputStream data = getClass().getClassLoader()
                            .getResourceAsStream(CLASSPATH_DIR + "/" + name)) {
                        if (data == null) {
                            logger.warning("Migration listed in index but missing: " + name);
                            continue;
                        }
                        byte[] bytes = data.readAllBytes();
                        Path tmp = Files.createTempFile("sapientia-mig-", ".sql");
                        Files.write(tmp, bytes);
                        tmp.toFile().deleteOnExit();
                        Path renamed = tmp.resolveSibling(name);
                        Files.move(tmp, renamed, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        renamed.toFile().deleteOnExit();
                        out.add(renamed);
                    }
                }
            }
        }
        return out;
    }

    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inLineComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append(c);
                }
                continue;
            }
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                inLineComment = true;
                i++;
                continue;
            }
            if (c == ';') {
                out.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            out.add(tail);
        }
        return out;
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record Migration(int version, String filename, String sql, String checksum) {}

    private record AppliedMigration(int version, String filename, String checksum) {}
}
