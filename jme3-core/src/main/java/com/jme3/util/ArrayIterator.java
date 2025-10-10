package com.jme3.util;

import java.util.Iterator;

public class ArrayIterator <T> implements Iterable<T>, Iterator<T> {

    private final T[] array;
    private int index = 0;

    @SafeVarargs
    public ArrayIterator(T... array) {
        this.array = array;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public T next() {
        return array[index++];
    }

}
