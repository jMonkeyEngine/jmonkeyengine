package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.SharingMode;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkPhysicalDeviceHdrVividFeaturesHUAWEI;

import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;

public class DeviceLocalBuffer extends AbstractVulkanBuffer {

    private BufferStream stream;
    private BufferHandle oldHandle;

    protected DeviceLocalBuffer(MemorySize size, BufferStream stream) {
        super(size);
        this.stream = stream;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped by the CPU.");
    }

    @Override
    protected BufferMapping mapNative(BufferHandle handle, long offset, long size) {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped by the CPU.");
    }

    @Override
    protected void moveToNewBuffer(BufferHandle oldHandle, BufferHandle newHandle) {
        this.oldHandle = oldHandle;
    }

    @Override
    public void stage(long offset, long size) {}

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
    public ResizeResult resize(MemorySize size) {
        this.size = size;
        if (size.getEnd() > getHandle().getMemory().getSize()) {
            initialize(null);
            return stream != null ? ResizeResult.Realloc : ResizeResult.DataLost;
        }
        return ResizeResult.Success;
    }

    @Override
    protected void initialize(LogicalDevice<?> device) {
        memProps = memProps.add(MemoryProp.DeviceLocal);
        super.initialize(device);
    }

    public void setStream(BufferStream stream) {
        this.stream = stream;
    }

    public BufferStream getStream() {
        return stream;
    }

    public static DeviceLocalBuffer build(MemorySize size, Consumer<Builder> config) {
        DeviceLocalBuffer b = new DeviceLocalBuffer(size, null);
        config.accept(b.new Builder());
        return b;
    }

    private class AliasBuffer implements VulkanBuffer {

        private final BufferHandle handle;
        private final MemorySize size;

        public AliasBuffer(BufferHandle handle) {
            this.handle = handle;
            this.size = MemorySize.bytes(handle.getMemory().getSize());
        }

        @Override
        public long getBufferId(LogicalDevice<?> device) {
            return handle.getNativeObject();
        }

        @Override
        public Flag<BufferUsage> getUsage() {
            return DeviceLocalBuffer.this.getUsage();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return handle.getMemory().getFlags();
        }

        @Override
        public IntEnum<SharingMode> getSharingMode() {
            return DeviceLocalBuffer.this.getSharingMode();
        }

        @Override
        public BufferMapping map(long offset, long size) {
            return mapNative(handle, offset, size);
        }

        @Override
        public void stage(long offset, long size) {}

        @Override
        public void upload(CommandBuffer cmd, BufferStream stream) {}

        @Override
        public ResizeResult resize(MemorySize size) {
            return ResizeResult.Failure;
        }

        @Override
        public MemorySize size() {
            return size;
        }

    }

}
