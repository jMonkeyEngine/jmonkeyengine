package com.jme3.util;

public class VersionedValue <T> {

    private T value;
    private long version = 0;

    public VersionedValue(T value) {
        this.value = value;
    }

    public void set(T value) {
        if (!this.value.equals(value)) {
            version++;
        }
        this.value = value;
    }

    public void update() {
        version++;
    }

    public T get() {
        return value;
    }

    public long getVersion() {
        return version;
    }

}
