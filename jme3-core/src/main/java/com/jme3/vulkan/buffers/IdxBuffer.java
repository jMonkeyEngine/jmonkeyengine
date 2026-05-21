package com.jme3.vulkan.buffers;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;

public class IdxBuffer <T extends MappableBuffer> implements MappableBuffer {

    @Final private IndexType type;
    @Final private T buffer;

    @SerializationOnly
    protected IdxBuffer() {}

    public IdxBuffer(IndexType type, T buffer) {
        this.type = type;
        this.buffer = buffer;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return buffer.map(offset, size);
    }

    @Override
    public void stage(long offset, long size) {
        buffer.stage(offset, size);
    }

    @Override
    public void resize(long bytes) {
        buffer.resize(bytes);
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(type, "type", IndexType.UInt32);
        out.write(buffer, "buffer", null);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        type = in.readEnum("type", IndexType.class, IndexType.UInt32);
        buffer = (T)in.readSavable("buffer", null);
    }

    public IndexType getType() {
        return type;
    }

    public T getBuffer() {
        return buffer;
    }

    public int getElements() {
        return (int)(size().getBytes() / type.getBytes());
    }

}
