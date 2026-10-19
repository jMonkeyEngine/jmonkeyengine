package com.jme3.util.collections;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class ArrayPool <T> implements Collection<T> {

    private Object[] array;
    private int size = 0;

    public ArrayPool() {
        this(8);
    }

    public ArrayPool(int initialCapacity) {
        array = new Object[initialCapacity];
    }

    private void grow(int requestedSize) {
        if (array.length < requestedSize) {
            Object[] temp = new Object[Math.max(array.length << 1, requestedSize)];
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(array[i], o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl();
    }

    @Override
    public Object[] toArray() {
        Object[] out = new Object[size];
        System.arraycopy(array, 0, out, 0, size);
        return out;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        if (a.length < size) {
            //noinspection unchecked
            a = (T1[])Array.newInstance(a.getClass().getComponentType(), size);
        }
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(array, 0, a, 0, size);
        return a;
    }

    @Override
    public boolean add(T t) {
        grow(size + 1);
        array[size++] = t;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(array[i], o)) {
                array[i] = i < --size ? array[size] : null;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        outer: for (Object o : c) {
            for (int i = 0; i < size; i++) {
                if (Objects.equals(array[i], o)) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        grow(size + c.size());
        for (Object o : c) {
            array[size++] = o;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed = remove(o) | changed;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        outer: for (int i = size - 1; i >= 0; i--) {
            for (Object o : c) {
                if (Objects.equals(array[i], o)) {
                    continue outer;
                }
            }
            array[i] = i < --size ? array[size] : null;
            changed = true;
        }
        return changed;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        size = 0;
    }

    private class IteratorImpl implements Iterator<T> {

        private int index = size - 1;

        @Override
        public boolean hasNext() {
            return index >= 0;
        }

        @Override @SuppressWarnings("unchecked")
        public T next() {
            return (T)array[index--];
        }

        @Override
        public void remove() {
            array[index] = index < --size ? array[size] : null;
        }

    }

}
