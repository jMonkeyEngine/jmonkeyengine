package com.jme3.vulkan.material.experimental;

import java.util.Objects;

public class MatParam <T> {

    private T value;
    private long version = 0;

    public MatParam(T value) {
        this.value = value;
    }

    public void set(T value) {
        if (!Objects.equals(this.value, value)) {
            version++;
        }
        this.value = value;
    }

    public void force() {
        version++;
    }

    public T get() {
        return value;
    }

    public long getVersion() {
        return version;
    }

}
