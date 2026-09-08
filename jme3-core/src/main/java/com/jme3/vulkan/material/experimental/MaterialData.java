package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.SparseStructList;
import com.jme3.vulkan.buffer.AutoBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.util.Flag;

import java.util.HashMap;
import java.util.Map;

public class MaterialData {

    private final MemoryAllocator allocator;
    private final Map<Class<? extends Struct>, AutoBuffer<SparseStructList>> buffers = new HashMap<>();

    public MaterialData(MemoryAllocator allocator) {
        this.allocator = allocator;
    }

    public void initialize(Struct struct, int length, Flag<EngineBuffer.Role> roles) {
        AutoBuffer<SparseStructList> b = buffers.get(struct.getClass());
        if (b == null) {
            buffers.put(struct.getClass(), new AutoBuffer<>(allocator, new SparseStructList<>(length, struct), BufferType.Dynamic, roles));
        } else {
            b.getStructure().setLength(Math.max(length, b.getStructure().getLength()));
            b.addRoles(roles);
        }
    }

    public int acquire(CommandBuffer cmd, Class<? extends Struct> type) {
        AutoBuffer<SparseStructList> b = buffers.get(type);
        int i = b.getStructure().acquireElement();
        b.update(cmd, OpLocation.PreferHost);
        return i;
    }

    public void release(Class<? extends Struct> type, int index) {
        AutoBuffer<SparseStructList> b = buffers.get(type);
        assert b != null : "Buffer for " + type + " does not exist.";
        b.getStructure().releaseElement(index);
    }

    @SuppressWarnings("unchecked")
    public <T extends Struct> T get(Class<T> type, int index) {
        return (T)buffers.get(type).getStructure().index(index);
    }

}
