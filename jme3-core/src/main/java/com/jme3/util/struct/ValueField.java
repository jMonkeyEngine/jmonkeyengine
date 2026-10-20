package com.jme3.util.struct;

public interface ValueField <T> extends StructField {

    void set(T value);

    T get(T store);

    T get();

}
