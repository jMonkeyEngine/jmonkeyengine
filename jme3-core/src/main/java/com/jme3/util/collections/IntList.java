package com.jme3.util.collections;

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
        size += list.size();
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
        for (int i = 0; i < size; i++) {
            if (array[i] == n) {
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

    /**
     * Stably sorts this list with powersort (Munro and Wild, 2018): natural runs are found (and
     * extended to a minimum length with binary insertion sort), and adjacent runs are merged in
     * the order given by the "power" of the boundary between them, which yields nearly optimal
     * merge costs and adapts to presorted input.
     *
     * <p>Note: fully built and tested by Claude. I did not review the code because I have no
     * idea how powersort works. Powersort is an improvement of timsort (used previously by jme3 for
     * sorting geometries), with O(nlogn) worst-case time complexity.</p>
     *
     * @param comparator element order
     * @see #sort(int, int, IntComparator)
     */
    public void sort(IntComparator comparator) {
        sort(0, size, comparator);
    }

    /**
     * Stably sorts the elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
     * with powersort, as {@link #sort(IntComparator)} does for the whole list. Elements outside
     * the range are left untouched.
     *
     * @param fromIndex index of the first element to sort
     * @param toIndex index after the last element to sort
     * @param comparator element order
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > size()} or
     *                                   {@code fromIndex > toIndex}
     */
    public void sort(int fromIndex, int toIndex, IntComparator comparator) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("from " + fromIndex + ", to " + toIndex + ", size " + size);
        }
        PowerSort.sort(array, fromIndex, toIndex, comparator);
    }

}
