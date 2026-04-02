package com.jme3.util.struct;

public interface StructSequence <T extends Struct> extends Iterable<Integer> {

    T get();

    void sample(int index);

    void increment();

    void decrement();

}
