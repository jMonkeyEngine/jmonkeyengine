package com.jme3.vulkan.material.uniforms;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.function.Function;

public class UniformBuffer <T extends Struct> implements MappableBuffer {

    private final Function<MemorySize, MappableBuffer> bufferGenerator;
    private final StructLayout layout;
    private final T struct;
    private final MappableBuffer buffer;
    private boolean synced = false;

    public UniformBuffer(StructLayout layout, T struct, Function<MemorySize, MappableBuffer> buffer) {
        this.bufferGenerator = buffer;
        this.struct = struct;
        this.layout = layout;
        this.buffer = buffer.apply(MemorySize.bytes(layout.getStructSize(struct)));
    }

    public UniformBuffer(UniformBuffer<T> src) {
        bufferGenerator = src.bufferGenerator;
        layout = src.layout;
        struct = (T)layout.newStruct(src.struct.getClass());
        buffer = bufferGenerator.apply(src.buffer.size());
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return buffer.map(offset, size);
    }

    @Override
    public void push(int offset, int size) {
        buffer.push(offset, size);
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public void write() {
        layout.updateBuffer(struct, buffer, !synced);
        synced = true;
    }

    public void read() {
        layout.updateStruct(buffer, struct);
        synced = true;
    }

    public T get() {
        return struct;
    }

    public MappableBuffer getBuffer() {
        return buffer;
    }

}
