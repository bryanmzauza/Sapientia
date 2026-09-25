package dev.brmz.sapientia.core.block;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The compact block index and its token-guarded chunk loading. */
final class ChunkBlockIndexTest {

    private static final Logger LOGGER = Logger.getLogger("ChunkBlockIndexTest");

    private final SapientiaBlock cable = block("cable");
    private final SapientiaBlock furnace = block("furnace");
    private final SapientiaBlockRegistry registry = new SapientiaBlockRegistry();
    private final ChunkBlockIndex index;
    private final List<String> seen = new ArrayList<>();

    ChunkBlockIndexTest() {
        registry.register(cable);
        registry.register(furnace);
        index = new ChunkBlockIndex(LOGGER, new CustomBlockStore(LOGGER, new NoDataSource()), registry);
        index.addObserver(new ChunkBlockIndex.Observer() {
            @Override public void onBlockAdded(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
                seen.add("+" + definition.id().value() + "@" + key.x() + "," + key.y() + "," + key.z());
            }
            @Override public void onBlockRemoved(@NotNull BlockKey key, @NotNull SapientiaBlock definition) {
                seen.add("-" + definition.id().value());
            }
            @Override public void onChunkUnloaded(@NotNull String world, int chunkX, int chunkZ) {
                seen.add("unload " + chunkX + "," + chunkZ);
            }
        });
    }

    @Test
    void storesAndFindsBlocksAcrossTheWholeHeightAndNegativeCoordinates() {
        index.put(new BlockKey("w", -1, -64, -1), cable);
        index.put(new BlockKey("w", -1, 319, -1), furnace);
        index.put(new BlockKey("w", 15, 0, 15), cable);

        assertThat(index.at(new BlockKey("w", -1, -64, -1))).isSameAs(cable);
        assertThat(index.at(new BlockKey("w", -1, 319, -1))).isSameAs(furnace);
        assertThat(index.at(new BlockKey("w", -1, 318, -1))).isNull();
        assertThat(index.at(new BlockKey("other", -1, 319, -1))).isNull();
        assertThat(index.chunkSize("w", -1, -1)).isEqualTo(2);
        assertThat(index.size()).isEqualTo(3);
    }

    @Test
    void countsBlocksPerTypeInAChunk() {
        for (int i = 0; i < 100; i++) index.put(new BlockKey("w", i % 16, 64 + i / 16, 0), cable);
        index.put(new BlockKey("w", 3, 10, 3), furnace);
        index.remove(new BlockKey("w", 0, 64, 0));

        assertThat(index.countsInChunk("w", 0, 0)).containsEntry(cable, 99).containsEntry(furnace, 1);
        assertThat(index.countsInChunk("w", 5, 5)).isEmpty();
    }

    @Test
    void replacingABlockReportsTheOldOne() {
        BlockKey key = new BlockKey("w", 1, 1, 1);
        index.put(key, cable);
        index.put(key, furnace);

        assertThat(seen).containsExactly("+cable@1,1,1", "-cable", "+furnace@1,1,1");
    }

    @Test
    void aReadAppliesOnlyWhileItsTokenIsCurrent() {
        long stale = index.beginLoad("w", 0, 0);
        long current = index.beginLoad("w", 0, 0); // chunk loaded again

        assertThat(index.apply("w", 0, 0, stale, List.of(stored(1, 64, 1, "cable")))).isFalse();
        assertThat(index.apply("w", 0, 0, current, List.of(stored(2, 64, 2, "cable")))).isTrue();

        assertThat(index.at(new BlockKey("w", 1, 64, 1))).isNull();
        assertThat(index.at(new BlockKey("w", 2, 64, 2))).isSameAs(cable);
    }

    @Test
    void unloadingDiscardsAReadInFlight() {
        long token = index.beginLoad("w", 0, 0);
        index.unloadChunk("w", 0, 0);

        assertThat(index.apply("w", 0, 0, token, List.of(stored(1, 64, 1, "cable")))).isFalse();
        assertThat(index.chunkSize("w", 0, 0)).isZero();
    }

    @Test
    void blocksPlacedWhileLoadingWinOverStoredOnes() {
        long token = index.beginLoad("w", 0, 0);
        index.put(new BlockKey("w", 1, 64, 1), furnace);

        index.apply("w", 0, 0, token, List.of(stored(1, 64, 1, "cable"), stored(4, 64, 4, "cable")));

        assertThat(index.at(new BlockKey("w", 1, 64, 1))).isSameAs(furnace);
        assertThat(index.at(new BlockKey("w", 4, 64, 4))).isSameAs(cable);
        index.unloadChunk("w", 0, 0);
        assertThat(seen).last().isEqualTo("unload 0,0");
    }

    private static CustomBlockStore.StoredBlock stored(int x, int y, int z, String id) {
        return new CustomBlockStore.StoredBlock(new BlockKey("w", x, y, z), "sapientia:" + id, null);
    }

    private static SapientiaBlock block(String name) {
        NamespacedKey id = new NamespacedKey("sapientia", name);
        return new SapientiaBlock() {
            @Override public @NotNull NamespacedKey id() { return id; }
            @Override public @NotNull Material baseMaterial() { return Material.STONE; }
            @Override public @NotNull String displayNameKey() { return "block." + name + ".name"; }
        };
    }

    private static final class NoDataSource implements javax.sql.DataSource {
        @Override public java.sql.Connection getConnection() throws java.sql.SQLException { throw new java.sql.SQLException("unused"); }
        @Override public java.sql.Connection getConnection(String u, String p) throws java.sql.SQLException { throw new java.sql.SQLException("unused"); }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return LOGGER; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
