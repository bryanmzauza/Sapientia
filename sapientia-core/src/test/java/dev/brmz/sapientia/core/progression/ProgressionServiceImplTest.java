package dev.brmz.sapientia.core.progression;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.DataSource;

import dev.brmz.sapientia.api.events.SapientiaDiscoveryEvent;
import dev.brmz.sapientia.api.events.SapientiaEraChangeEvent;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.core.persistence.MigrationLoader;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

final class ProgressionServiceImplTest {

    private static final Logger LOGGER = Logger.getLogger("ProgressionServiceImplTest");
    private static final UUID PLAYER = UUID.randomUUID();

    private static final NamespacedKey WORKBENCH = key("workbench");
    private static final NamespacedKey COPPER_DUST = key("copper_dust");
    private static final NamespacedKey TIN_DUST = key("tin_dust");
    private static final NamespacedKey BRONZE_DUST = key("bronze_dust");
    private static final NamespacedKey STEEL = key("stainless_steel_ingot");
    private static final NamespacedKey BRONZE_RECIPE = key("recipe_alloy_bronze");
    private static final NamespacedKey STEEL_RECIPE = key("recipe_steel");

    @TempDir
    Path dir;

    private final List<Event> events = new ArrayList<>();
    private final Map<NamespacedKey, Era> eras = new HashMap<>();
    private ProgressionStore store;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("test.db");
        DataSource ds = new FileDataSource(url);
        try (Connection c = ds.getConnection()) {
            new MigrationLoader(LOGGER).applyAll(c);
        }
        store = new ProgressionStore(LOGGER, ds);
        eras.put(WORKBENCH, Era.ARRIVAL);
        eras.put(COPPER_DUST, Era.COPPER_AGE);
        eras.put(TIN_DUST, Era.BRONZE_AGE);
        eras.put(BRONZE_DUST, Era.BRONZE_AGE);
        eras.put(STEEL, Era.STEEL_AGE);
    }

    private ProgressionServiceImpl service(Era era, boolean research) {
        ProgressionServiceImpl service = new ProgressionServiceImpl(store, Runnable::run, Runnable::run, era, research,
                eras::get, eras::containsKey, events::add, uuid -> true);
        service.setRecipes(() -> List.of(
                new ResearchBook.Entry(BRONZE_RECIPE, BRONZE_DUST, Set.of(COPPER_DUST, TIN_DUST)),
                new ResearchBook.Entry(STEEL_RECIPE, STEEL, Set.of())), () -> 1);
        return service;
    }

    @Test
    void erasUpToTheServerEraAreUnlocked() {
        ProgressionServiceImpl service = service(Era.BRONZE_AGE, true);

        assertThat(service.isUnlocked(Era.ARRIVAL)).isTrue();
        assertThat(service.isUnlocked(Era.BRONZE_AGE)).isTrue();
        assertThat(service.isUnlocked(Era.IRON_AGE)).isFalse();
        assertThat(service.isAvailable(STEEL)).isFalse();
        assertThat(service.eraOf(key("unknown"))).isEqualTo(Era.ARRIVAL);
    }

    @Test
    void changingTheEraIsSavedAndAnnounced() {
        ProgressionServiceImpl service = service(Era.STONE_AGE, true);

        service.setServerEra(Era.COPPER_AGE);

        assertThat(service.serverEra()).isEqualTo(Era.COPPER_AGE);
        assertThat(store.loadEra()).isEqualTo(Era.COPPER_AGE);
        assertThat(events).singleElement().isInstanceOfSatisfying(SapientiaEraChangeEvent.class, e -> {
            assertThat(e.previous()).isEqualTo(Era.STONE_AGE);
            assertThat(e.current()).isEqualTo(Era.COPPER_AGE);
        });
    }

    @Test
    void recipesUnlockWhenEveryIngredientIsDiscovered() {
        ProgressionServiceImpl service = service(Era.BRONZE_AGE, true);
        assertThat(service.isRecipeUnlocked(PLAYER, BRONZE_RECIPE)).isFalse();
        assertThat(service.missingFor(PLAYER, BRONZE_RECIPE)).containsExactlyInAnyOrder(COPPER_DUST, TIN_DUST);

        service.discover(PLAYER, COPPER_DUST);
        assertThat(service.isRecipeUnlocked(PLAYER, BRONZE_RECIPE)).isFalse();
        service.discover(PLAYER, TIN_DUST);

        assertThat(service.isRecipeUnlocked(PLAYER, BRONZE_RECIPE)).isTrue();
        SapientiaDiscoveryEvent last = (SapientiaDiscoveryEvent) events.get(events.size() - 1);
        assertThat(last.item()).isEqualTo(TIN_DUST);
        assertThat(last.unlockedRecipes()).containsExactly(BRONZE_RECIPE);
    }

    @Test
    void recipesOfLockedErasStayLockedEvenWithoutIngredients() {
        ProgressionServiceImpl service = service(Era.BRONZE_AGE, true);

        assertThat(service.isRecipeUnlocked(PLAYER, STEEL_RECIPE)).isFalse();
        service.setServerEra(Era.STEEL_AGE);
        assertThat(service.isRecipeUnlocked(PLAYER, STEEL_RECIPE)).isTrue();
    }

    @Test
    void withoutResearchEveryRecipeOfAnUnlockedEraIsAvailable() {
        ProgressionServiceImpl service = service(Era.BRONZE_AGE, false);

        assertThat(service.isRecipeUnlocked(PLAYER, BRONZE_RECIPE)).isTrue();
        assertThat(service.missingFor(PLAYER, BRONZE_RECIPE)).isEmpty();
    }

    @Test
    void discoveriesAreWrittenAndLoadedBack() {
        ProgressionServiceImpl service = service(Era.BRONZE_AGE, true);
        assertThat(service.discover(PLAYER, COPPER_DUST)).isTrue();
        assertThat(service.discover(PLAYER, COPPER_DUST)).isFalse();
        store.flush();

        ProgressionServiceImpl restarted = service(Era.BRONZE_AGE, true);
        boolean[] loaded = {false};
        restarted.load(PLAYER, () -> loaded[0] = true);

        assertThat(loaded[0]).isTrue();
        assertThat(restarted.hasDiscovered(PLAYER, COPPER_DUST)).isTrue();
        restarted.evict(PLAYER);
        assertThat(restarted.hasDiscovered(PLAYER, COPPER_DUST)).isFalse();
    }

    @Test
    void theNextGoalIsTheFirstGatewayNotYetMade() {
        ProgressionServiceImpl service = service(Era.STEEL_AGE, true);
        assertThat(service.nextGoal(PLAYER)).isEqualTo(Era.ARRIVAL); // workbench

        service.discover(PLAYER, WORKBENCH);
        // Gateways of eras 1 to 9 are not registered in this test, so the next goal is era 10's.
        assertThat(service.nextGoal(PLAYER)).isEqualTo(Era.STEEL_AGE);

        service.discover(PLAYER, STEEL);
        assertThat(service.nextGoal(PLAYER)).isNull();
    }

    private static NamespacedKey key(String id) {
        return new NamespacedKey("sapientia", id);
    }

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
