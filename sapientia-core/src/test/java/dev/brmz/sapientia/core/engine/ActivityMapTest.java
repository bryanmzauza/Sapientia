package dev.brmz.sapientia.core.engine;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ActivityMapTest {

    private final List<String> events = new ArrayList<>();
    private final ActivityMap.Listener recorder = new ActivityMap.Listener() {
        @Override public void onChunkActivated(String world, int x, int z) { events.add("+" + world + ":" + x + "," + z); }
        @Override public void onChunkDeactivated(String world, int x, int z) { events.add("-" + world + ":" + x + "," + z); }
    };

    @Test
    void playerActivatesASquareOfChunksAroundThem() {
        ActivityMap map = new ActivityMap(4, 600, recorder);
        map.addViewer("world", 0, 0, 0);

        assertThat(map.isActive("world", 4, -4)).isTrue();
        assertThat(map.isActive("world", 5, 0)).isFalse();
        assertThat(map.activeCount()).isEqualTo(81);
        assertThat(events).hasSize(81);
    }

    @Test
    void chunksNextToThePlayerStayActiveWhenTheyStepAway() {
        ActivityMap map = new ActivityMap(4, 600, recorder);
        map.addViewer("world", 0, 0, 0);
        events.clear();

        map.moveViewer("world", 0, 0, "world", 1, 0, 10);

        // Only the far column changes; the chunk the player left stays active.
        assertThat(map.isActive("world", 0, 0)).isTrue();
        assertThat(events).hasSize(9).allMatch(e -> e.startsWith("+world:5,"));
    }

    @Test
    void chunksDeactivateOnlyAfterTheDelay() {
        ActivityMap map = new ActivityMap(1, 600, recorder);
        map.addViewer("world", 0, 0, 0);
        map.removeViewer("world", 0, 0, 100);
        events.clear();

        map.sweep(699);
        assertThat(map.isActive("world", 0, 0)).isTrue();
        assertThat(events).isEmpty();

        map.sweep(700);
        assertThat(map.isActive("world", 0, 0)).isFalse();
        assertThat(events).hasSize(9).allMatch(e -> e.startsWith("-"));
    }

    @Test
    void returningBeforeTheDelayCancelsTheDeactivation() {
        ActivityMap map = new ActivityMap(1, 600, recorder);
        map.addViewer("world", 0, 0, 0);
        map.removeViewer("world", 0, 0, 100);
        events.clear();

        map.addViewer("world", 0, 0, 200);
        map.sweep(10_000);

        assertThat(events).isEmpty();
        assertThat(map.isActive("world", 0, 0)).isTrue();
    }

    @Test
    void overlappingPlayersKeepSharedChunksActive() {
        ActivityMap map = new ActivityMap(1, 0, recorder);
        map.addViewer("world", 0, 0, 0);
        map.addViewer("world", 2, 0, 0); // shares column x=1

        map.removeViewer("world", 0, 0, 0);

        assertThat(map.isActive("world", 1, 0)).isTrue();
        assertThat(map.isActive("world", -1, 0)).isFalse();
    }

    @Test
    void changingWorldMovesTheWholeSquare() {
        ActivityMap map = new ActivityMap(0, 0, recorder);
        map.addViewer("world", 0, 0, 0);

        map.moveViewer("world", 0, 0, "world_nether", 0, 0, 0);

        assertThat(map.isActive("world", 0, 0)).isFalse();
        assertThat(map.isActive("world_nether", 0, 0)).isTrue();
    }
}
