package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.ReservedStructArray;
import com.jme3.vulkan.buffer.AutoBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.util.Flag;

public class MaterialBuffer<T extends Struct> {

    private final AutoBuffer<ReservedStructArray<T>> buffer;

    public MaterialBuffer(MemoryAllocator allocator, int elements, T struct, BufferType type, Flag<GpuBuffer.Role> roles) {
        this.buffer = new AutoBuffer<>(allocator, new ReservedStructArray<>(elements, struct), type, roles);
    }

    public int acquire(CommandBuffer cmd) {
        int i = buffer.getStructure().acquireElement();
        buffer.update(cmd, OpLocation.PreferHost);
        return i;
    }

    public void release(int index) {
        buffer.getStructure().releaseElement(index);
    }

    public T index(int index) {
        return buffer.getStructure().index(index);
    }

}
