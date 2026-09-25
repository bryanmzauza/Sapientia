package dev.brmz.sapientia.core.collect;

import java.util.Arrays;

/** Growable list of primitive ints; removal swaps in the last element (order is not kept). */
public final class IntList {

    private int[] items = new int[4];
    private int size;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int get(int index) {
        return items[index];
    }

    public void add(int value) {
        if (size == items.length) {
            items = Arrays.copyOf(items, size << 1);
        }
        items[size++] = value;
    }

    public void set(int index, int value) {
        items[index] = value;
    }

    /** Removes and returns the last element. */
    public int removeLast() {
        return items[--size];
    }

    public void clear() {
        size = 0;
    }

    /** Removes one occurrence of {@code value}; returns whether it was present. */
    public boolean removeValue(int value) {
        for (int i = 0; i < size; i++) {
            if (items[i] == value) {
                items[i] = items[--size];
                return true;
            }
        }
        return false;
    }
}
