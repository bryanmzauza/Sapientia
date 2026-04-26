package dev.brmz.sapientia.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.core.block.BlockKey;
import org.junit.jupiter.api.Test;

final class WriteBehindQueueDedupeTest {

    @Test
    void lastWriteWinsWithinSingleBucket() {
        WriteBehindQueue queue = new WriteBehindQueue(Logger.getLogger("test"), new NoopDataSource());
        BlockKey k = new BlockKey("world", 1, 2, 3);

        for (int i = 0; i < 500; i++) {
            queue.enqueuePut(k, "sapientia:wrench", null);
        }
        assertThat(queue.pendingCount()).isEqualTo(1);

        queue.enqueueRemove(k);
        assertThat(queue.pendingCount()).isEqualTo(1);
    }

    private static final class NoopDataSource implements DataSource {
        @Override public Connection getConnection() throws SQLException { throw new SQLException("unused"); }
        @Override public Connection getConnection(String u, String p) throws SQLException { throw new SQLException("unused"); }
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
}
