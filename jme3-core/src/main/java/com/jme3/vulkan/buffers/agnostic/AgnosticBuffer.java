package com.jme3.vulkan.buffers.agnostic;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;

public class AgnosticBuffer <T extends MappableBuffer> implements MappableBuffer {

    @Final private Flag<BufferUsage> usage;
    @Final private UpdateHint update;
    @Final private MappableBuffer buffer;

    @SerializationOnly
    protected AgnosticBuffer() {}

    public AgnosticBuffer(long bytes, Flag<BufferUsage> usage, UpdateHint update) {
        this.usage = usage;
        this.update = update;
        this.buffer = JmePlatform.allocateStandardBuffer(bytes, usage, update);
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
        out.write(size().getBytes(), "size", 0L);
        out.write(usage.bits(), "usage", BufferUsage.Vertex.bits());
        out.write(update, "hint", UpdateHint.Dynamic);
        try (BufferMapping m = map()) {
            out.write(m.getBytes(), "data", null);
        }
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        long bytes = in.readLong("size", 0L);
        usage = Flag.of(in.readInt("usage", BufferUsage.Vertex.bits()));
        update = in.readEnum("hint", UpdateHint.class, UpdateHint.Dynamic);
        buffer = JmePlatform.allocateStandardBuffer(bytes, usage, update);
        try (BufferMapping m = buffer.map()) {
            MemoryUtil.memCopy(in.readByteBuffer("data", null), m.getBytes());
        }
    }

}
