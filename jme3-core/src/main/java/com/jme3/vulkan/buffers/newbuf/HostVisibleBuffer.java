package com.jme3.vulkan.buffers.newbuf;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.SourceBufferMapping;
import com.jme3.vulkan.buffers.alloc.BufferAllocRequest;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class HostVisibleBuffer extends AbstractVulkanBuffer {

    @SerializationOnly
    protected HostVisibleBuffer() {
        super(MemoryProp.HostVisibleAndCoherent);
    }

    protected HostVisibleBuffer(long bytes) {
        super(bytes, MemoryProp.HostVisibleAndCoherent);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        try (BufferMapping m = map()) {
            out.write(m.getBytes(), "bytes", null);
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        ByteBuffer bytes = in.readByteBuffer("bytes", null);
        try (BufferMapping m = map()) {
            MemoryUtil.memCopy(bytes, m.getBytes());
        }
    }

    @Override
    protected BufferMapping mapNative(BufferHandle handle, long offset, long size) {
        return new SourceBufferMapping(this, handle.getMemory().map(this.size.getOffset() + offset, size),
                size, () -> handle.getMemory().unmap());
    }

    @Override
    protected void moveToNewBuffer(BufferHandle oldHandle, BufferHandle newHandle) {
        long size = Math.min(oldHandle.getMemory().getSize(), newHandle.getMemory().getSize());
        try (BufferMapping srcMap = mapNative(oldHandle, 0, size); BufferMapping dstMap = mapNative(newHandle, 0, size)) {
            MemoryUtil.memCopy(srcMap.getBytes(), dstMap.getBytes());
        }
    }

    public static HostVisibleBuffer build(long bytes, Consumer<Builder> config) {
        HostVisibleBuffer b = new HostVisibleBuffer(bytes);
        config.accept(b.new Builder());
        return b;
    }

    public static class Alloc implements BufferAllocRequest<DeviceLocalBuffer> {

        @Final private Flag<BufferUsage> usage;

        @SerializationOnly
        protected Alloc() {}

        public Alloc(Flag<BufferUsage> usage) {
            this.usage = usage;
        }

        @Override
        public DeviceLocalBuffer create(long bytes) {
            return DeviceLocalBuffer.build(bytes, b -> b.setUsage(usage));
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(usage.bits(), "usage", BufferUsage.Vertex.bits());
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            usage = Flag.of(in.readInt("usage", BufferUsage.Vertex.bits()));
        }

    }

}
