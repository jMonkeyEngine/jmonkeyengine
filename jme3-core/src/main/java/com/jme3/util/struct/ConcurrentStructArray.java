package com.jme3.util.struct;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Struct array that concurrent on element accesses.
 *
 * @param <T>
 */
public class ConcurrentStructArray <T extends Struct> extends SingletonStructArray<ConcurrentElement<T>> {

    private final Supplier<T> factory;
    private final Queue<ConcurrentElement<T>> available = new ConcurrentLinkedQueue<>();
    private StructLayout layout;
    private long currentLayoutVersion = 0;

    public ConcurrentStructArray(int length, Supplier<T> factory) {
        super(length, new ConcurrentElement<>(factory.get(), e -> {}));
        this.factory = factory;
    }

    @Override
    public void setupLayout(StructLayout layout) {
        this.layout = layout;
        currentLayoutVersion++;
        super.setupLayout(layout);
    }

    @Override
    public ConcurrentElement<T> index(int i) {
        if (i >= length()) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length());
        }
        ConcurrentElement<T> struct = available.poll();
        if (struct == null) {
            struct = new ConcurrentElement<>(factory.get(), this::releaseElement);
        }
        struct.bind(layout, currentLayoutVersion, binding.getBuffer(), binding.getOffset() + stride * i);
        return struct;
    }

    private void releaseElement(ConcurrentElement<T> el) {
        available.add(el);
    }

}
