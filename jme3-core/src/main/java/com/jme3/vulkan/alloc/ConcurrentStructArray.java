package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffer.EngineBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class ConcurrentStructArray <T extends Struct> extends StructArray<T> {

    private final IntFunction<T> factory;
    private Index<T>[] structs;

    public ConcurrentStructArray(int length, IntFunction<T> factory) {
        this(length, factory, null);
    }

    public ConcurrentStructArray(int length, IntFunction<T> factory, EngineBuffer source) {
        super(length, factory.apply(0), source);
        this.factory = factory;
        //noinspection unchecked
        structs = new Index[length];
        structs[0] = getSharedStruct();
        for (int i = 1; i < length; i++) {
            structs[i] = new Index<>(factory.apply(i), i * getByteStride());
        }
    }

    @Override
    public T index(int index) {
        return structs[index].getStruct();
    }

    @Override
    public void setLength(int length) {
        super.setLength(length);
        if (length > structs.length) {
            //noinspection unchecked
            Index<T>[] temp = new Index[pickNextSize(structs.length, length)];
            System.arraycopy(structs, 0, temp, 0, structs.length);
            structs = temp;
        }
    }

    protected int pickNextSize(int currentSize, int requestedSize) {
        // next power of two at or above requestedSize
        return Math.max(Integer.highestOneBit(requestedSize - 1) << 1, Math.max(1, currentSize));
    }

}
