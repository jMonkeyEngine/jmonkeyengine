package com.jme3.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Ring buffer (fixed size queue) implementation using a circular array (array
 * with wrap-around).
 */
// suppress unchecked warnings in Java 1.5.0_6 and later
@SuppressWarnings("unchecked")
public class RingBuffer<T> implements Iterable<T> {

    private T[] buffer;          // queue elements
    private int count = 0;          // number of elements on queue
    private int indexOut = 0;       // index of first element of queue
    private int indexIn = 0;       // index of next available slot

    // cast needed since no generic array creation in Java
    public RingBuffer(int capacity) {
        buffer = (T[]) new Object[capacity];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int size() {
        return count;
    }

    public void push(T item) {
        if (count == buffer.length) {
            throw new RuntimeException("Ring buffer overflow");
        }
        buffer[indexIn] = item;
        indexIn = (indexIn + 1) % buffer.length;     // wrap-around
        count++;
    }

    public T pop() {
        if (isEmpty()) {
            throw new RuntimeException("Ring buffer underflow");
        }
        T item = buffer[indexOut];
        buffer[indexOut] = null;                  // to help with garbage collection
        count--;
        indexOut = (indexOut + 1) % buffer.length; // wrap-around
        return item;
    }

    public Iterator<T> iterator() {
        return new RingBufferIterator();
    }

    // an iterator, doesn't implement remove() since it's optional
    private class RingBufferIterator implements Iterator<T> {

        private int i = 0;

        public boolean hasNext() {
            return i < count;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return buffer[i++];
        }
    }
}
