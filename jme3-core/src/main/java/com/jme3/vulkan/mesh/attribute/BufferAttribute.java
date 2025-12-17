package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.mesh.VertexBinding;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public abstract class BufferAttribute <T, B extends Buffer> extends AbstractAttribute<T> {

    public BufferAttribute(VertexBinding binding, GpuBuffer vertices, int size, int offset) {
        super(binding, vertices, size, offset);
    }

    public void set(int baseElement, B values) {
        ByteBuffer buf = getBuffer(baseElement);
        int length = values.remaining() * Float.BYTES;
        if (length > buf.remaining()) {
            throw new BufferOverflowException();
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(values), MemoryUtil.memAddress(buf), length);
    }

    public B get(int baseElement, B store) {
        ByteBuffer buf = getBuffer(baseElement);
        int length = store.remaining() * Float.BYTES;
        if (length > buf.remaining()) {
            throw new BufferOverflowException();
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(buf), MemoryUtil.memAddress(store), length);
        return store;
    }

}
