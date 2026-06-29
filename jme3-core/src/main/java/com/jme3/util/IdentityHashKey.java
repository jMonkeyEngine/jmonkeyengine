package com.jme3.util;

/**
 * Class whose {@link #equals(Object)} and {@link #hashCode()} are based on
 * the enclosed object's identity rather than the enclosed object's equals
 * and hashcode methods.
 *
 * @param <T>
 */
public class IdentityHashKey <T> {

    private final T key;

    public IdentityHashKey(T key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentityHashKey that = (IdentityHashKey)o;
        return key == that.key;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(key);
    }

    public T get() {
        return key;
    }

}
