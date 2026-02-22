package com.jme3.vulkan.buffers;

import com.jme3.export.*;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.EffectivelyFinal;
import com.jme3.vulkan.tmp.EffectivelyFinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.*;

public class NioBuffer implements MappableBuffer, Disposable, Savable {

    @EffectivelyFinal
    protected DisposableReference ref;

    private ByteBuffer buffer;
    private MemorySize size;

    @SerializationOnly
    protected NioBuffer() {}

    public NioBuffer(MemorySize size) {
        this(size, true);
    }

    public NioBuffer(MemorySize size, boolean clearMem) {
        this.size = size;
        if (clearMem) {
            buffer = MemoryUtil.memCalloc((int)size.getBytes());
        } else {
            buffer = MemoryUtil.memAlloc((int)size.getBytes());
        }
        ref = DisposableManager.reference(this);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        ByteBuffer data = buffer.position((int)size.getOffset()).limit((int)size.getEnd()).slice();
        out.write(data, "data", null);
        out.write(size, "size", null);
        out.write(buffer.capacity(), "capacity", (int)size.getEnd());
    }

    @Override
    @EffectivelyFinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        size = (MemorySize)in.readSavable("size", null);
        ByteBuffer data = in.readByteBuffer("data", null);
        int capacity = in.readInt("capacity", (int)(data.capacity() + size.getOffset()));
        buffer = MemoryUtil.memAlloc(capacity).position((int)size.getOffset()).limit((int)size.getEnd());
        MemoryUtil.memCopy(data, buffer);
        ref = DisposableManager.reference(this);
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (offset == 0) {
            return new VirtualBufferMapping(buffer.duplicate());
        } else {
            return new VirtualBufferMapping(buffer.position((int)offset).limit((int)(offset + size)).slice());
        }
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> MemoryUtil.memFree(buffer);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    @Override
    public void stage(long offset, long size) {}

    @Override
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (size.getBytes() > buffer.capacity()) {
            ByteBuffer newBuffer = MemoryUtil.memRealloc(buffer, (int)size.getBytes());
            if (newBuffer != buffer) {
                buffer = newBuffer;
                ref.refresh();
            }
            return ResizeResult.Realloc;
        }
        return ResizeResult.Success;
    }

}
