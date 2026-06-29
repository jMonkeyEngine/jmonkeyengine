package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffers.MappableBuffer;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

public class DataGroup implements Mappable {

    private final Map<Class, StructArray> arrays = new ConcurrentHashMap<>();
    private final BitSet indices = new BitSet();

    @Override
    public void map(MappingArena arena) {
        for (StructArray a : arrays.values()) {
            a.map(arena);
        }
    }

    public void addArrayIfAbsent(Struct struct, IntFunction<MappableBuffer> bufferFactory) {
        arrays.computeIfAbsent(struct.getClass(), k -> new StructArray(struct, 100, bufferFactory));
    }

    public <T extends Struct> T getStruct(Class<T> type, int index) {
        return (T)arrays.get(type).index(index);
    }

    public int acquireIndex() {
        int i;
        synchronized (indices) {
            // The buffer needs to be resized if the index is outside the pre-allocated bounds.
            // My current approach is to call resize(size) on the buffer and the buffer itself
            // manages the necessary creation, copy, and teardown. I would prefer to resize the
            // StructArray and have the underlying buffer resize as a result, so then I wouldn't
            // have to track the underlying buffers. Unfortunately there doesn't seem to be a
            // reliable way to pull that off.
            indices.set(i = indices.nextClearBit(0));
        }
        return i;
    }

    public void releaseIndex(int i) {
        synchronized (indices) {
            indices.clear(i);
        }
    }

}
