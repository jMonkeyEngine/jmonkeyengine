package com.jme3.vulkan.buffers.newbuf;

import com.jme3.export.*;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.SharingMode;
import com.jme3.vulkan.buffers.alloc.BufferAllocRequest;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;

public class DeviceLocalBuffer extends AbstractVulkanBuffer {

    private BufferHandle oldHandle;

    @SerializationOnly
    protected DeviceLocalBuffer() {
        super(MemoryProp.DeviceLocal);
    }

    protected DeviceLocalBuffer(long bytes) {
        super(bytes, MemoryProp.DeviceLocal);
    }

    @Override
    protected BufferMapping mapNative(BufferHandle handle, long offset, long size) {
        throw new UnsupportedOperationException("Device local memory cannot be mapped.");
    }

    @Override
    protected void moveToNewBuffer(BufferHandle oldHandle, BufferHandle newHandle) {
        this.oldHandle = oldHandle;
    }

    @Override
    public void upload(CommandBuffer cmd, BufferStream stream) {
        if (oldHandle != null) try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(Math.min(getHandle().getMemory().getSize(), oldHandle.getMemory().getSize()));
            vkCmdCopyBuffer(cmd.getBuffer(), oldHandle.getNativeObject(), getHandle().getNativeObject(), copy);
            oldHandle = null;
        }
    }

    @Override
    public void resize(long bytes) {
        size = size.setBytes(bytes);
        if (getHandle() != null && size.getEnd() > getHandle().getMemory().getSize()) {
            initialize(null);
        }
    }

    public static DeviceLocalBuffer build(long bytes, Consumer<Builder> config) {
        DeviceLocalBuffer b = new DeviceLocalBuffer(bytes);
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
            return build(bytes, b -> b.setUsage(usage));
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(usage.bits(), "usage", BufferUsage.Vertex.bits());
        }

        @Override
        @FinalWriter
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            usage = Flag.of(in.readInt("usage", BufferUsage.Vertex.bits()));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Alloc that = (Alloc)o;
            return Flag.is(usage, that.usage);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(usage.bits());
        }

    }

}
