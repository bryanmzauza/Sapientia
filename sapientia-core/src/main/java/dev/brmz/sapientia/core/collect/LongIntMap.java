package dev.brmz.sapientia.core.collect;

import java.util.Arrays;

/**
 * Open-addressing hash map from primitive {@code long} keys to {@code int}
 * values, with no boxing and no entry objects. Absent keys read as
 * {@link #MISSING}. Not thread-safe.
 */
public final class LongIntMap {

    /** Value returned for absent keys. */
    public static final int MISSING = -1;

    private static final long FREE = Long.MIN_VALUE;
    private static final float LOAD_FACTOR = 0.6f;

    private long[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int resizeAt;
    private boolean hasFreeKey;
    private int freeValue;

    public LongIntMap() {
        allocate(16);
    }

    public int size() {
        return size + (hasFreeKey ? 1 : 0);
    }

    public int get(long key) {
        if (key == FREE) {
            return hasFreeKey ? freeValue : MISSING;
        }
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) return MISSING;
            if (k == key) return values[i];
            i = (i + 1) & mask;
        }
    }

    public boolean containsKey(long key) {
        return key == FREE ? hasFreeKey : contains(key);
    }

    /** Associates {@code value} with {@code key}; returns the previous value or {@link #MISSING}. */
    public int put(long key, int value) {
        if (key == FREE) {
            int previous = hasFreeKey ? freeValue : MISSING;
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
                    rehash(keys.length << 1);
                }
                return MISSING;
            }
            if (k == key) {
                int previous = values[i];
                values[i] = value;
                return previous;
            }
            i = (i + 1) & mask;
        }
    }

    /** Removes {@code key}; returns its value or {@link #MISSING}. */
    public int remove(long key) {
        if (key == FREE) {
            int previous = hasFreeKey ? freeValue : MISSING;
            hasFreeKey = false;
            return previous;
        }
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) return MISSING;
            if (k == key) break;
            i = (i + 1) & mask;
        }
        int previous = values[i];
        size--;
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
        return previous;
    }

    private boolean contains(long key) {
        int i = slot(key);
        while (true) {
            long k = keys[i];
            if (k == FREE) return false;
            if (k == key) return true;
            i = (i + 1) & mask;
        }
    }

    private int slot(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32)) & mask;
    }

    private void allocate(int capacity) {
        keys = new long[capacity];
        Arrays.fill(keys, FREE);
        values = new int[capacity];
        mask = capacity - 1;
        resizeAt = (int) (capacity * LOAD_FACTOR);
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        int[] oldValues = values;
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
