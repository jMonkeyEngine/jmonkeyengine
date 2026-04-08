package com.jme3.util;

public interface SafeCloseable extends AutoCloseable {

    @Override
    void close();

}
