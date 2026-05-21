package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.buffers.MappableBuffer;

import java.util.*;

public class MaterialDataManager {

    private final Map<Class<? extends ShadingInterface>, InterfaceData> data = new HashMap<>();

    private static class InterfaceData {

        private final BitSet filledIndices = new BitSet();
        private final Map<Class<? extends Struct>, MappableBuffer> buffers = new HashMap<>();

        public int handshake() {
            int index = filledIndices.nextClearBit(0);
            filledIndices.set(index);
            return index;
        }

        public void leave(int index) {
            filledIndices.clear(index);
        }

        public <T extends Struct> StructMapping<T> map(Class<T> dataType, int index) {

        }

    }

    private static class Buffer {

        private final Struct struct;
        private final MappableBuffer buffer;

    }

    public static class DataHandle <T extends Struct> {

        private final int index;

    }

}
