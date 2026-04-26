package dev.brmz.sapientia.core.petroleum;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.core.persistence.MigrationLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ReservoirServiceTest {

    private static final Logger LOGGER = Logger.getLogger(ReservoirServiceTest.class.getName());

    @BeforeAll
    static void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    void initialisesDeterministicAmountInRange() {
        ReservoirService.Reservoir.class.getName(); // sanity load
        int amount = ReservoirService.deterministicInitial(
                new ReservoirService.ChunkRef("world", 0, 0));
        assertThat(amount).isBetween(ReservoirService.MIN_INIT_MB, ReservoirService.MAX_INIT_MB);
    }

    @Test
    void deterministicValuesAreStableAcrossInvocations() {
        var ref = new ReservoirService.ChunkRef("world", 7, -3);
        int a = ReservoirService.deterministicInitial(ref);
        int b = ReservoirService.deterministicInitial(ref);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentChunksProduceDifferentSeeds() {
        int a = ReservoirService.deterministicInitial(new ReservoirService.ChunkRef("world", 1, 0));
        int b = ReservoirService.deterministicInitial(new ReservoirService.ChunkRef("world", 0, 1));
        // Vanishingly small chance of equality with the FNV+xor mix; acceptable risk.
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void drainReducesAmountAndPersists() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new MigrationLoader(LOGGER).applyAll(connection);
            ReservoirService svc = new ReservoirService(LOGGER, new SingleConnectionDataSource(connection));
            int initial = svc.amount("world", 5, 5);
            int drawn   = svc.drain("world", 5, 5, 250);
            assertThat(drawn).isEqualTo(250);
            assertThat(svc.amount("world", 5, 5)).isEqualTo(initial - 250);
        }
    }

    @Test
    void drainCapsAtAvailable() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new MigrationLoader(LOGGER).applyAll(connection);
            ReservoirService svc = new ReservoirService(LOGGER, new SingleConnectionDataSource(connection));
            int initial = svc.amount("world", 1, 1);
            int drawn   = svc.drain("world", 1, 1, initial + 99_999);
            assertThat(drawn).isEqualTo(initial);
            assertThat(svc.amount("world", 1, 1)).isEqualTo(0);
        }
    }

    private static final class SingleConnectionDataSource implements DataSource {
        private final Connection connection;
        SingleConnectionDataSource(Connection c) { this.connection = c; }
        @Override public Connection getConnection() { return new UnclosableConnection(connection); }
        @Override public Connection getConnection(String u, String p) { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return new PrintWriter(System.err); }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    private static final class UnclosableConnection implements Connection {
        private final Connection delegate;
        UnclosableConnection(Connection delegate) { this.delegate = delegate; }
        @Override public void close() { /* swallow — owner closes */ }
        // Everything else delegates.
        @Override public java.sql.Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean b) throws SQLException { delegate.setAutoCommit(b); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
        @Override public java.sql.DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String c) throws SQLException { delegate.setCatalog(c); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public java.sql.Statement createStatement(int rt, int rc) throws SQLException { return delegate.createStatement(rt, rc); }
        @Override public java.sql.PreparedStatement prepareStatement(String s, int rt, int rc) throws SQLException { return delegate.prepareStatement(s, rt, rc); }
        @Override public java.sql.CallableStatement prepareCall(String s, int rt, int rc) throws SQLException { return delegate.prepareCall(s, rt, rc); }
        @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> m) throws SQLException { delegate.setTypeMap(m); }
        @Override public void setHoldability(int h) throws SQLException { delegate.setHoldability(h); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public java.sql.Savepoint setSavepoint(String n) throws SQLException { return delegate.setSavepoint(n); }
        @Override public void rollback(java.sql.Savepoint s) throws SQLException { delegate.rollback(s); }
        @Override public void releaseSavepoint(java.sql.Savepoint s) throws SQLException { delegate.releaseSavepoint(s); }
        @Override public java.sql.Statement createStatement(int rt, int rc, int rh) throws SQLException { return delegate.createStatement(rt, rc, rh); }
        @Override public java.sql.PreparedStatement prepareStatement(String s, int rt, int rc, int rh) throws SQLException { return delegate.prepareStatement(s, rt, rc, rh); }
        @Override public java.sql.CallableStatement prepareCall(String s, int rt, int rc, int rh) throws SQLException { return delegate.prepareCall(s, rt, rc, rh); }
        @Override public java.sql.PreparedStatement prepareStatement(String s, int agk) throws SQLException { return delegate.prepareStatement(s, agk); }
        @Override public java.sql.PreparedStatement prepareStatement(String s, int[] ci) throws SQLException { return delegate.prepareStatement(s, ci); }
        @Override public java.sql.PreparedStatement prepareStatement(String s, String[] cn) throws SQLException { return delegate.prepareStatement(s, cn); }
        @Override public java.sql.Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public java.sql.Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public java.sql.NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int t) throws SQLException { return delegate.isValid(t); }
        @Override public void setClientInfo(String n, String v) throws java.sql.SQLClientInfoException { delegate.setClientInfo(n, v); }
        @Override public void setClientInfo(java.util.Properties p) throws java.sql.SQLClientInfoException { delegate.setClientInfo(p); }
        @Override public String getClientInfo(String n) throws SQLException { return delegate.getClientInfo(n); }
        @Override public java.util.Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public java.sql.Array createArrayOf(String tn, Object[] el) throws SQLException { return delegate.createArrayOf(tn, el); }
        @Override public java.sql.Struct createStruct(String tn, Object[] at) throws SQLException { return delegate.createStruct(tn, at); }
        @Override public void setSchema(String s) throws SQLException { delegate.setSchema(s); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor e) throws SQLException { delegate.abort(e); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor e, int m) throws SQLException { delegate.setNetworkTimeout(e, m); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }
}
