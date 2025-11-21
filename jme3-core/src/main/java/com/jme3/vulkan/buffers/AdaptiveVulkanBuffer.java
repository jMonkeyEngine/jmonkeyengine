package com.jme3.vulkan.buffers;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.GlVertexBuffer.Usage;
import com.jme3.texture.GlFrameBuffer;
import com.jme3.vulkan.allocate.ResourceTicket;
import com.jme3.vulkan.allocate.ResourceWrapper;
import com.jme3.vulkan.allocate.StaticAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.update.Command;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class AdaptiveVulkanBuffer implements VulkanBuffer, AdaptiveBuffer, Command {

    private final LogicalDevice<?> device;
    private final StaticAllocator<VulkanBuffer> allocator;
    private final UpdateFrameManager frames;
    private GlVertexBuffer.Usage mode;

    private VulkanBuffer gpuOnlyBuffer;
    private NioBuffer cpuOnlyBuffer;
    private final SharedBuffer[] sharedBuffers;

    private Flag<BufferUsage> usage = BufferUsage.Storage;
    private boolean concurrent = false;

    public AdaptiveVulkanBuffer(LogicalDevice<?> device, StaticAllocator<VulkanBuffer> allocator, UpdateFrameManager frames) {
        this.device = device;
        this.allocator = allocator;
        this.frames = frames;
        this.sharedBuffers = new SharedBuffer[frames.getTotalFrames()];
    }

    @Override
    public void setAccessMode(Usage mode) {

    }

    @Override
    public Usage getAccessMode() {
        return null;
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return device;
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return usage;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        switch (mode) {
            case Stream: return MemoryProp.HostVisible.add(MemoryProp.HostCoherent);
            case Dynamic:
            case Static: return MemoryProp.DeviceLocal;
            case CpuOnly: return Flag.empty();
            default: throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return cpuOnlyBuffer.map(offset, size);
    }

    @Override
    public long getId() {
        switch (mode) {
            case Stream: return sharedBuffers[frames.getCurrentFrame()].get().getId();
            case Dynamic:
            case Static: return gpuOnlyBuffer.getId();
            case CpuOnly: return MemoryUtil.NULL;
            default: throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean resize(MemorySize size) {
        switch (mode) {
            case Stream: {
                boolean resize = false;
                for (int i = 0; i < sharedBuffers.length; i++) {
                    sharedBuffers[i] = new PersistentSharedBuffer(allocator.allocate());
                }
            }
        }
    }

    @Override
    public MemorySize size() {
        return null;
    }

    @Override
    public void unmap() {

    }

    @Override
    public boolean requiresCommandBuffer(int frame) {
        return false;
    }

    @Override
    public void run(CommandBuffer cmd, int frame) {

    }

    private static class Ticket implements ResourceTicket<VulkanBuffer> {

        private final MemorySize size;
        private final Flag<BufferUsage> usage;

        private final boolean concurrent;

        private Ticket(MemorySize size, Flag<BufferUsage> usage, boolean concurrent) {
            this.size = size;
            this.usage = usage;
            this.concurrent = concurrent;
        }

        @Override
        public Float selectResource(VulkanBuffer resource) {
            if ()
        }

        @Override
        public VulkanBuffer createResource() {
            return null;
        }

    }

    private static class SharedBuffer {

        protected final ResourceWrapper<? extends VulkanBuffer> wrapper;

        private SharedBuffer(ResourceWrapper<? extends VulkanBuffer> wrapper) {
            this.wrapper = wrapper;
        }

        public GpuBuffer get() {
            return wrapper.get();
        }

    }

    private static class PersistentSharedBuffer extends SharedBuffer {

        private final PersistentVulkanBuffer<VulkanBuffer> buffer;

        private PersistentSharedBuffer(ResourceWrapper<? extends VulkanBuffer> wrapper) {
            super(wrapper);
            buffer = new PersistentVulkanBuffer<>(wrapper.get());
        }

        @Override
        public VulkanBuffer get() {
            return buffer;
        }

    }

}
