package com.jme3.util.struct;

import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.util.Objects;
import java.util.function.Function;

public class StructBuffer <S extends Struct, B extends MappableBuffer> implements MappableBuffer {

    private final StructLayout layout;
    private final S struct;
    private final B buffer;

    public StructBuffer(StructLayout layout, S struct, Function<MemorySize, B> buffer) {
        this.layout = layout;
        this.struct = Objects.requireNonNull(struct);
        this.buffer = buffer.apply(MemorySize.bytes(layout.getStructSize(struct)));
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
    public ResizeResult resize(MemorySize size) {
        return buffer.resize(size);
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public void updateBuffer() {
        updateBuffer(false);
    }

    public void updateBuffer(boolean force) {
        int size = layout.getStructSize(struct);
        if (size > buffer.size().getBytes()) {
            buffer.resize(MemorySize.dynamic(size, buffer.size().getBytesPerElement()));
        }
        layout.updateBuffer(struct, buffer, force);
    }

    public void updateStruct() {
        layout.updateStruct(buffer, struct);
    }

    public StructLayout getLayout() {
        return layout;
    }

    public S getStruct() {
        return struct;
    }

    public B getBuffer() {
        return buffer;
    }

}
