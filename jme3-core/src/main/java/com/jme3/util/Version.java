package com.jme3.util;

import java.util.Objects;

public class Version <T extends Versionable> implements Versionable {

    private final T object;
    private long version = -1L;

    public Version(T object) {
        this.object = object;
    }

    public boolean updateNeeded() {
        return version < object.getVersionNumber();
    }

    public boolean update() {
        long prev = version;
        version = object.getVersionNumber();
        return prev < version;
    }

    public T get() {
        return object;
    }

    @Override
    public long getVersionNumber() {
        return object.getVersionNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Version<?> version = (Version<?>) o;
        return Objects.equals(object, version.object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }

    public static <T extends Versionable> T get(Version<T> v) {
        return v != null ? v.get() : null;
    }

}
