package dev.brmz.sapientia.core.persistence;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.api.logistics.ItemNodeType;
import dev.brmz.sapientia.core.block.BlockKey;
import dev.brmz.sapientia.core.energy.EnergyNodeStore;
import dev.brmz.sapientia.core.energy.SimpleEnergyNode;
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import dev.brmz.sapientia.core.fluids.FluidNodeStore;
import dev.brmz.sapientia.core.fluids.SimpleFluidNode;
import dev.brmz.sapientia.core.logistics.ItemNodeStore;
import dev.brmz.sapientia.core.logistics.SimpleItemNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Node stores against a real SQLite file: queued writes, chunk columns and chunk reads. */
final class NodeStoresTest {

    private static final Logger LOGGER = Logger.getLogger("NodeStoresTest");

    @TempDir
    Path dir;

    private DataSource dataSource;

    @BeforeEach
    void migrate() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("test.db");
        dataSource = new FileDataSource(url);
        try (Connection c = dataSource.getConnection()) {
            new MigrationLoader(LOGGER).applyAll(c);
        }
    }

    @Test
    void sqliteShiftsNegativeCoordinatesToTheRightChunk() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT -1 >> 4, -16 >> 4, -17 >> 4, 15 >> 4")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(-1);
            assertThat(rs.getInt(2)).isEqualTo(-1);
            assertThat(rs.getInt(3)).isEqualTo(-2);
            assertThat(rs.getInt(4)).isZero();
        }
    }

    @Test
    void energyWritesAreQueuedCollapsedAndReadBackByChunk() {
        EnergyNodeStore store = new EnergyNodeStore(LOGGER, dataSource);
        SimpleEnergyNode inChunk = energy(-1, 64, -1, 5);
        SimpleEnergyNode other = energy(-17, 64, -1, 7);
        store.put(inChunk);
        store.put(inChunk);
        store.put(other);
        assertThat(store.writes().pendingCount()).isEqualTo(2);
        assertThat(store.loadChunk("w", -1, -1)).isEmpty(); // nothing written yet

        store.writes().flush();

        List<SimpleEnergyNode> loaded = store.loadChunk("w", -1, -1);
        assertThat(loaded).singleElement().satisfies(n -> {
            assertThat(n.nodeId()).isEqualTo(inChunk.nodeId());
            assertThat(n.bufferCurrent()).isEqualTo(5);
        });
        assertThat(store.loadChunk("w", -2, -1)).hasSize(1);

        store.delete(inChunk.location());
        store.writes().flush();
        assertThat(store.loadChunk("w", -1, -1)).isEmpty();
    }

    @Test
    void theLatestBufferIsWrittenAtFlushTime() {
        EnergyNodeStore store = new EnergyNodeStore(LOGGER, dataSource);
        SimpleEnergyNode node = energy(3, 64, 3, 0);
        store.put(node);
        node.offer(40);

        store.writes().flush();

        assertThat(store.loadChunk("w", 0, 0).get(0).bufferCurrent()).isEqualTo(40);
    }

    @Test
    void replacingABlockBeforeTheFlushKeepsOnlyTheNewNode() {
        EnergyNodeStore store = new EnergyNodeStore(LOGGER, dataSource);
        SimpleEnergyNode first = energy(1, 64, 1, 0);
        store.put(first);
        store.writes().flush();
        SimpleEnergyNode second = energy(1, 64, 1, 0);
        store.delete(first.location());
        store.put(second);

        store.writes().flush();

        assertThat(store.loadChunk("w", 0, 0)).singleElement()
                .satisfies(n -> assertThat(n.nodeId()).isEqualTo(second.nodeId()));
    }

    @Test
    void itemAndFluidNodesRoundTrip() {
        ItemNodeStore items = new ItemNodeStore(LOGGER, dataSource);
        SimpleItemNode item = new SimpleItemNode(UUID.randomUUID(), new BlockKey("w", 20, 64, 20),
                ItemNodeType.CONSUMER, EnergyTier.MID, 3);
        items.put(item);
        items.writes().flush();
        assertThat(items.loadChunk("w", 1, 1)).singleElement()
                .satisfies(n -> assertThat(n.priority()).isEqualTo(3));

        FluidNodeStore fluids = new FluidNodeStore(LOGGER, dataSource);
        SimpleFluidNode tank = new SimpleFluidNode(UUID.randomUUID(), new BlockKey("w", -5, 10, 40),
                FluidNodeType.TANK, EnergyTier.LOW, null, 0);
        tank.offer(BuiltinFluidTypes.WATER, 250);
        fluids.put(tank);
        tank.offer(BuiltinFluidTypes.WATER, 250); // after the put: the queued row keeps 250
        fluids.writes().flush();

        List<SimpleFluidNode> loaded = fluids.loadChunk("w", -1, 2, id -> BuiltinFluidTypes.WATER);
        assertThat(loaded).singleElement().satisfies(n -> {
            assertThat(n.contents()).isNotNull();
            assertThat(n.contents().amountMb()).isEqualTo(250);
        });
    }

    private static SimpleEnergyNode energy(int x, int y, int z, long buffer) {
        return new SimpleEnergyNode(UUID.randomUUID(), new BlockKey("w", x, y, z),
                EnergyNodeType.CAPACITOR, EnergyTier.LOW, buffer, 1_000);
    }

    /** Opens a fresh connection per call, like the plugin's pool. */
    private record FileDataSource(String url) implements DataSource {
        @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url); }
        @Override public Connection getConnection(String u, String p) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return LOGGER; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
