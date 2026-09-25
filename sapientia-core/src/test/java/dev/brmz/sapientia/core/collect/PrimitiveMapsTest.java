package dev.brmz.sapientia.core.collect;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The primitive maps behave like {@link HashMap} under random puts and removals. */
final class PrimitiveMapsTest {

    @Test
    void longObjectMapMatchesHashMap() {
        LongObjectMap<String> map = new LongObjectMap<>();
        Map<Long, String> reference = new HashMap<>();
        SplittableRandom random = new SplittableRandom(42);
        for (int i = 0; i < 200_000; i++) {
            long key = key(random);
            switch (random.nextInt(3)) {
                case 0, 1 -> assertThat(map.put(key, "v" + i)).isEqualTo(reference.put(key, "v" + i));
                default -> assertThat(map.remove(key)).isEqualTo(reference.remove(key));
            }
        }
        assertThat(map.size()).isEqualTo(reference.size());
        for (Map.Entry<Long, String> entry : reference.entrySet()) {
            assertThat(map.get(entry.getKey())).isEqualTo(entry.getValue());
        }
        int[] visited = {0};
        map.forEachValue(v -> visited[0]++);
        assertThat(visited[0]).isEqualTo(reference.size());
    }

    @Test
    void longIntMapMatchesHashMap() {
        LongIntMap map = new LongIntMap();
        Map<Long, Integer> reference = new HashMap<>();
        SplittableRandom random = new SplittableRandom(7);
        for (int i = 0; i < 200_000; i++) {
            long key = key(random);
            if (random.nextInt(3) != 2) {
                Integer previous = reference.put(key, i);
                assertThat(map.put(key, i)).isEqualTo(previous == null ? LongIntMap.MISSING : previous);
            } else {
                Integer previous = reference.remove(key);
                assertThat(map.remove(key)).isEqualTo(previous == null ? LongIntMap.MISSING : previous);
            }
        }
        assertThat(map.size()).isEqualTo(reference.size());
        for (Map.Entry<Long, Integer> entry : reference.entrySet()) {
            assertThat(map.get(entry.getKey())).isEqualTo(entry.getValue());
            assertThat(map.containsKey(entry.getKey())).isTrue();
        }
    }

    @Test
    void intListRemovesBySwapping() {
        IntList list = new IntList();
        for (int i = 0; i < 10; i++) list.add(i);
        assertThat(list.removeValue(3)).isTrue();
        assertThat(list.removeValue(3)).isFalse();
        assertThat(list.size()).isEqualTo(9);
        assertThat(list.get(3)).isEqualTo(9);
    }

    /** Small key space so keys repeat, plus the sentinel value used for free slots. */
    private static long key(SplittableRandom random) {
        return random.nextInt(50) == 0 ? Long.MIN_VALUE : random.nextLong(-5_000, 5_000) * 0x10001L;
    }
}
