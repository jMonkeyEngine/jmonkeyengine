package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.Mappable;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public abstract class BufferAttribute <T, B extends Buffer> extends AbstractAttribute<T> {

    public BufferAttribute(Mappable vertices, int size, int stride, int offset) {
        super(vertices, size, stride, offset);
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
