package com.jme3.vulkan.mesh.attribute;

public interface Attribute <T> extends Iterable<T> {

    void unmap();

    void set(int element, T value);

    T get(int element);

    default void set(int startElement, T[] values) {
        for (int i = 0; i < values.length; i++) {
            set(startElement + i, values[i]);
        }
    }

    default T get(int element, T store) {
        return get(element);
    }

    default T[] get(int startElement, T[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = get(startElement + i, store[i]);
        }
        return store;
    }

}
