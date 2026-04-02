package com.jme3.vulkan.buffers;

import com.jme3.export.*;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.*;

public class NioBuffer implements MappableBuffer, Disposable, Savable {

    private final boolean clearMem;
    @Final protected DisposableReference ref;

    private ByteBuffer buffer;
    private MemorySize size;

    public NioBuffer() {
        this(1, true);
    }

    public NioBuffer(long bytes) {
        this(bytes, true);
    }

    public NioBuffer(boolean clearMem) {
        this(1, clearMem);
    }

    public NioBuffer(long bytes, boolean clearMem) {
        this.size = new MemorySize(bytes);
        this.clearMem = clearMem;
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
    @FinalWriter
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
        if (buffer == null) {
            if (ref != null) {
                ref.destroy();
            }
            if (clearMem) {
                buffer = MemoryUtil.memCalloc((int)this.size.getEnd());
            } else {
                buffer = MemoryUtil.memAlloc((int)this.size.getEnd());
            }
            ref = DisposableManager.reference(this);
        }
        if (offset == 0) {
            return new DirectBufferMapping(buffer.duplicate());
        } else {
            return new DirectBufferMapping(buffer.position((int)(this.size.getOffset() + offset))
                    .limit((int)(this.size.getOffset() + offset + size)).slice());
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
    public void resize(long bytes) {
        size = size.setBytes(bytes);
        if (buffer != null && size.getEnd() > buffer.capacity()) {
            ByteBuffer newBuffer = MemoryUtil.memRealloc(buffer, (int)size.getEnd());
            if (newBuffer != buffer) {
                buffer = newBuffer;
                ref.refresh();
            }
        }
    }

}
