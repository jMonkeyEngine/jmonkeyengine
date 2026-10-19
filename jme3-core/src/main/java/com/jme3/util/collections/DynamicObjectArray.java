package com.jme3.util.collections;

import java.util.Objects;

public class DynamicObjectArray <T> implements DynamicArray<T> {

    private Object[] array;
    private int size = 0;
    private final int initialSize;

    public DynamicObjectArray(int initialSize) {
        this.initialSize = initialSize;
    }

    private void grow(int requestedElement) {
        if (array == null) {
            array = new Object[Math.max(initialSize, requestedElement + 1)];
        } else if (requestedElement >= array.length) {
            Object[] temp = new Object[Math.max(array.length << 1, requestedElement + 1)];
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
    }

    @Override
    public void set(int i, T v) {
        grow(i);
        array[i] = v;
        if (v != null) {
            size = Math.max(size, i + 1);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public T get(int i) {
        return array == null || i >= array.length ? null : (T)array[i];
    }

    @Override
    public boolean isNull(int i) {
        return array == null || i >= array.length || array[i] == null;
    }

    @Override
    public int indexOf(T v) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(array[i], v)) {
                return i;
            }
        }
        return v != null ? -1 : size;
    }

}
