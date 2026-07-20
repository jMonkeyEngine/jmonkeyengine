package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.StructuredArray;

import java.util.BitSet;
import java.util.function.IntFunction;

public class MaterialDataBuffer <T extends StructuredArray> {

    private final IntFunction<T> factory;
    private final BitSet usedSlots = new BitSet();
    private T struct;

    public MaterialDataBuffer(IntFunction<T> factory) {
        this.factory = factory;
    }

    public T index(int i) {
        if (struct == null) {
            struct = factory.apply(32);
        } else if (i >= struct.getLength()) {
            T temp = factory.apply(struct.getLength() << 1);
            struct.copyArraysTo(temp);
            struct = temp;
        }
    }

}
