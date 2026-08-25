package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffer.EngineBuffer;

import java.util.BitSet;

public class ReservedStructArray <T extends Struct> extends StructArray<T> {

    private final BitSet reserved = new BitSet();

    public ReservedStructArray(int length, T struct) {
        super(length, struct);
    }

    public ReservedStructArray(int length, T struct, EngineBuffer source) {
        super(length, struct, source);
    }

    public ReservedStructArray(int length, ReservedStructArray<T> array) {
        super(length, array.getStruct());
        reserved.or(array.reserved);
    }

    public int acquire() {
        int i = reserved.nextClearBit(0);
        reserved.set(i);
        return i;
    }

    public void release(int index) {
        reserved.clear(index);
    }

    public int available() {
        return getLength() - reserved.cardinality();
    }

}
