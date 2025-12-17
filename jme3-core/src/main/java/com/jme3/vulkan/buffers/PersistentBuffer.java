package com.jme3.vulkan.buffers;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PersistentBuffer <T extends GpuBuffer> implements GpuBuffer, Native<T> {

    private final T buffer;
    private final PointerBuffer pointer = MemoryUtil.memCallocPointer(1);
    private final NativeReference ref;
    private ByteBuffer mapping;

    public PersistentBuffer(T buffer) {
        this.buffer = buffer;
        this.ref = Native.get().register(this);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (mapping == null) {
            mapping = buffer.mapBytes();
        }
        return pointer.put(0, MemoryUtil.memAddress(mapping, offset));
    }

    @Override
    public void push(int offset, int size) {
        buffer.push(offset, size);
    }

    @Override
    public long getId() {
        return buffer.getId();
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public void unmap() {}

    @Override
    public T getNativeObject() {
        return buffer;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> MemoryUtil.memFree(pointer);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void forceUnmap() {
        if (mapping != null) {
            mapping = null;
            buffer.unmap();
        }
    }

    public T getBuffer() {
        return buffer;
    }

}
