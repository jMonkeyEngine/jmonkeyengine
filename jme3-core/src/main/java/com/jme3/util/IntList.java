package com.jme3.util;

import java.util.Iterator;

public class IntList {

    private int[] array;
    private int size = 0;

    public IntList() {
        this(16);
    }

    public IntList(int initialCapacity) {
        array = new int[initialCapacity];
    }

    private static int[] grow(int[] array) {
        int[] temp = new int[array.length << 1];
        System.arraycopy(array, 0, temp, 0, array.length);
        return temp;
    }

    private static int[] grow(int targetCapacity) {
        return new int[Integer.highestOneBit(targetCapacity - 1) << 1];
    }

    public void add(int n) {
        if (size == array.length) {
            array = grow(array);
        }
        array[size++] = n;
    }

    public void add(int index, int n) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        if (size == array.length) {
            array = grow(array);
        }
        if (index < size) {
            System.arraycopy(array, index, array, index + 1, size - index);
        }
        array[index] = n;
        size++;
    }

    public void addFirst(int n) {
        add(0, n);
    }

    public void addAll(IntList list) {
        if (list.isEmpty()) {
            return;
        }
        if (array.length - size < list.size()) {
            array = grow(size + list.size());
        }
        System.arraycopy(list.array, 0, array, size, list.size());
    }

    public void set(int index, int n) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        array[index] = n;
    }

    public void removeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        if (index < size - 1) {
            System.arraycopy(array, index + 1, array, index, size - index - 1);
        }
        size--;
    }

    public boolean remove(int n) {
        for (int i = 0; i < size; i++) {
            if (array[i] == n) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    public int removeAll(int n) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (array[i] == n) {
                removeAt(i--);
                count++;
            }
        }
        return count;
    }

    public void removeFirst() {
        removeAt(0);
    }

    public void removeLast() {
        if (size == 0) {
            throw new IllegalStateException("No element to remove.");
        }
        size--;
    }

    public void clear() {
        size = 0;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        return array[index];
    }

    public boolean contains(int n) {
        for (int e : array) {
            if (e == n) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

}
