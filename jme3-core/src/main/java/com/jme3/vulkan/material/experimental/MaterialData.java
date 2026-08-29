package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.AutoBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MaterialData {

    private final Map<Class, MatBuffer<?>> data = new HashMap<>();

    public <T extends Struct> void initDataType(Class<T> type, Supplier<AutoBuffer<StructArray<T>>> factory) {
        data.computeIfAbsent(type, k -> new MatBuffer<>(factory.get()));
    }

    public int acquireElement(CommandBuffer cmd, Class type) {
        return data.get(type).acquireElement(cmd);
    }

    public void releaseElement(Class type, int element) {
        data.get(type).releaseElement(element);
    }

    @SuppressWarnings("unchecked")
    public <T extends Struct> T getStruct(Class<T> type, int index) {
        return (T)data.get(type).data.getStructure().index(index);
    }

    private static class MatBuffer <T extends Struct> {

        private final AutoBuffer<StructArray<T>> data;
        private final BitSet usedElements = new BitSet();

        public MatBuffer(AutoBuffer<StructArray<T>> data) {
            this.data = data;
        }

        public int acquireElement(CommandBuffer cmd) {
            int i = usedElements.nextClearBit(0);
            usedElements.set(i);
            if (i >= data.getStructure().getLength()) {
                data.update(cmd, new StructArray<>(i << 1, data.getStructure().getStruct()), OpLocation.PreferHost);
            }
            return i;
        }

        public void releaseElement(int element) {
            usedElements.clear(element);
        }

    }

}
