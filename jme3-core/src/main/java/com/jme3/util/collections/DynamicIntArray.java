package com.jme3.util.collections;

import java.util.BitSet;

public class DynamicIntArray implements DynamicArray<Integer> {

    private int[] array;
    private final BitSet nonNull = new BitSet();
    private final int initialSize;

    public DynamicIntArray(int initialSize) {
        this.initialSize = initialSize;
    }

    private void grow(int requestedElement) {
        if (array == null) {
            array = new int[Math.max(initialSize, requestedElement + 1)];
        } else if (requestedElement >= array.length) {
            int[] temp = new int[Math.max(array.length << 1, requestedElement + 1)];
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
    }

    @Override
    public void set(int i, Integer v) {
        grow(i);
        nonNull.set(i, v != null);
        if (v != null) array[i] = v;
    }

    @Override
    public Integer get(int i) {
        return nonNull.get(i) ? array[i] : null;
    }

    @Override
    public boolean isNull(int i) {
        return !nonNull.get(i);
    }

    @Override
    public int indexOf(Integer v) {
        if (v == null) {
            return nonNull.nextClearBit(0);
        }
        for (int i = nonNull.nextSetBit(0); i >= 0; i = nonNull.nextSetBit(i + 1)) {
            if (array[i] == v) {
                return i;
            }
        }
        return -1;
    }

}
