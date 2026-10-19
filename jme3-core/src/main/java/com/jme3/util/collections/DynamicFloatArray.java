package com.jme3.util.collections;

import java.util.BitSet;

public class DynamicFloatArray implements DynamicArray<Float> {

    private float[] array;
    private final BitSet nonNull = new BitSet();
    private final int initialSize;

    public DynamicFloatArray(int initialSize) {
        this.initialSize = initialSize;
    }

    private void grow(int requestedElement) {
        if (array == null) {
            array = new float[Math.max(initialSize, requestedElement + 1)];
        } else if (requestedElement >= array.length) {
            float[] temp = new float[Math.max(array.length << 1, requestedElement + 1)];
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
    }

    @Override
    public void set(int i, Float v) {
        nonNull.set(i, v != null);
        if (v != null) {
            grow(i);
            array[i] = v;
        }
    }

    @Override
    public Float get(int i) {
        return nonNull.get(i) ? array[i] : null;
    }

    @Override
    public boolean isNull(int i) {
        return !nonNull.get(i);
    }

    @Override
    public int indexOf(Float v) {
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
