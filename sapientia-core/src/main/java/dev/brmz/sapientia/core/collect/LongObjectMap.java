package dev.brmz.sapientia.core.collect;

import java.util.Arrays;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Open-addressing hash map from primitive {@code long} keys to values, with no
 * boxing and no entry objects. Network graphs key it by packed block or chunk
 * positions (see {@link dev.brmz.sapientia.core.engine.BlockPositions}), so a
 * neighbour lookup allocates nothing. Not thread-safe.
 */
public final class LongObjectMap<V> {

    private static final long FREE = Long.MIN_VALUE;
    private static final float LOAD_FACTOR = 0.6f;

    private long[] keys;
    private Object[] values;
    private int mask;
    private int size;
    private int resizeAt;

    // FREE marks empty slots, so a FREE key is stored on the side.
    private boolean hasFreeKey;
    private @Nullable Object freeValue;

    public LongObjectMap() {
        this(16);
    }

    public LongObjectMap(int expected) {
        int capacity = 8;
        while (capacity * LOAD_FACTOR < expected) {
            capacity <<= 1;
        }
        allocate(capacity);
    }

    public int size() {
        return size + (hasFreeKey ? 1 : 0);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("unchecked")
    public @Nullable V get(long key) {
        if (key == FREE) {
            return hasFreeKey ? (V) freeValue : null;
        }
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) return null;
            if (k == key) return (V) values[i];
            i = (i + 1) & mask;
        }
    }

    /** Associates {@code value} with {@code key}; returns the previous value, if any. */
    @SuppressWarnings("unchecked")
    public @Nullable V put(long key, V value) {
        if (key == FREE) {
            V previous = hasFreeKey ? (V) freeValue : null;
            hasFreeKey = true;
            freeValue = value;
            return previous;
        }
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) {
                keys[i] = key;
                values[i] = value;
                if (++size >= resizeAt) {
                    allocateAndRehash(keys.length << 1);
                }
                return null;
            }
            if (k == key) {
                V previous = (V) values[i];
                values[i] = value;
                return previous;
            }
            i = (i + 1) & mask;
        }
    }

    /** Removes {@code key}; returns its value, if it was present. */
    @SuppressWarnings("unchecked")
    public @Nullable V remove(long key) {
        if (key == FREE) {
            V previous = hasFreeKey ? (V) freeValue : null;
            hasFreeKey = false;
            freeValue = null;
            return previous;
        }
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) return null;
            if (k == key) break;
            i = (i + 1) & mask;
        }
        V previous = (V) values[i];
        size--;
        // Backward-shift deletion keeps every probe chain unbroken without tombstones.
        int gap = i;
        int j = i;
        while (true) {
            j = (j + 1) & mask;
            long k = keys[j];
            if (k == FREE) break;
            int home = slot(k);
            if (((j - home) & mask) >= ((j - gap) & mask)) {
                keys[gap] = k;
                values[gap] = values[j];
                gap = j;
            }
        }
        keys[gap] = FREE;
        values[gap] = null;
        return previous;
    }

    @SuppressWarnings("unchecked")
    public void forEachValue(Consumer<? super V> action) {
        if (hasFreeKey) action.accept((V) freeValue);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != FREE) action.accept((V) values[i]);
        }
    }

    private int slot(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32)) & mask;
    }

    private void allocate(int capacity) {
        keys = new long[capacity];
        Arrays.fill(keys, FREE);
        values = new Object[capacity];
        mask = capacity - 1;
        resizeAt = (int) (capacity * LOAD_FACTOR);
    }

    private void allocateAndRehash(int capacity) {
        long[] oldKeys = keys;
        Object[] oldValues = values;
        allocate(capacity);
        for (int i = 0; i < oldKeys.length; i++) {
            long k = oldKeys[i];
            if (k == FREE) continue;
            int j = slot(k);
            while (keys[j] != FREE) {
                j = (j + 1) & mask;
            }
            keys[j] = k;
            values[j] = oldValues[i];
        }
    }
}
