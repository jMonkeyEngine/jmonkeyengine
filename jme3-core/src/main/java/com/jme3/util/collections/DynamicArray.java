package com.jme3.util.collections;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Collection that behaves like a standard array, except it resizes to meet
 * requests that fall outside its current allocated range.
 *
 * @param <T>
 */
public interface DynamicArray <T> {

    void set(int i, T v);

    T get(int i);

    boolean isNull(int i);

    int indexOf(T v);

    default T computeIfNull(int i, Supplier<T> compute) {
        T v = get(i);
        if (v == null) {
            set(i, v = compute.get());
        }
        return v;
    }

    default T computeIfNull(int i, IntFunction<T> compute) {
        T v = get(i);
        if (v == null) {
            set(i, v = compute.apply(i));
        }
        return v;
    }

    default boolean fillIfNull(int i, Supplier<T> compute) {
        if (isNull(i)) {
            set(i, compute.get());
            return true;
        }
        return false;
    }

    default boolean fillIfNull(int i, IntFunction<T> compute) {
        if (isNull(i)) {
            set(i, compute.apply(i));
            return true;
        }
        return false;
    }

    default boolean clear(int i, Consumer<T> consume) {
        T v = get(i);
        if (v != null) {
            set(i, null);
            consume.accept(v);
            return true;
        }
        return false;
    }

    default boolean clear(int i, BiConsumer<Integer, T> consume) {
        T v = get(i);
        if (v != null) {
            set(i, null);
            consume.accept(i, v);
            return true;
        }
        return false;
    }

}
