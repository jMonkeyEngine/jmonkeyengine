package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;

import java.util.*;

public class ParameterPool {

    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<String, Collection<ParameterBuffer>> buffers = new HashMap<>();

    public void set(String name, Object value) {
        parameters.put(name, value);
    }

    public <T> T get(String name) {
        return (T)parameters.get(name);
    }

    public <T extends Struct> StructMapping<T> mapBuffer(String name, T struct) {
        Collection<ParameterBuffer> bufs = buffers.computeIfAbsent(name, k -> new ArrayList<>());
        for (ParameterBuffer b : bufs) {
            MappableBuffer result = b.compatible(struct);
            if (result != null) return result.mapStruct(struct);
        }
        ParameterBuffer newBuf = new ParameterBuffer(struct.getClass(), struct.getLayout(),
                JmePlatform.allocateStandardBuffer(struct.getSize(), BufferUsage.Uniform, UpdateHint.Dynamic));
        bufs.add(newBuf);
        return newBuf.buffer.mapStruct(struct);
    }

    private static class ParameterBuffer {

        private Class<? extends Struct> type;
        private final StructLayout layout;
        private final MappableBuffer buffer;

        public ParameterBuffer(Class<? extends Struct> type, StructLayout layout, MappableBuffer buffer) {
            this.buffer = buffer;
            this.layout = layout;
            this.type = type;
        }

        public MappableBuffer compatible(Struct struct) {
            if (struct.getLayout() != layout) {
                return null;
            }
            if (struct.getClass().isAssignableFrom(type)) {
                return buffer;
            }
            if (type.isAssignableFrom(struct.getClass())) {
                this.type = struct.getClass();
                buffer.resize(struct.getSize());
                return buffer;
            }
            return null;
        }

    }

}
