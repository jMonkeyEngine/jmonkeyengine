package com.jme3.util;

public interface ResourceHandle<T> extends AutoCloseable {

    @Override
    void close();

    T get();

}
