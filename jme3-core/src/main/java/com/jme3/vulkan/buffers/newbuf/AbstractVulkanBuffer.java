package com.jme3.vulkan.buffers.newbuf;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.LongBuffer;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public abstract class AbstractVulkanBuffer extends AbstractNative<Long> implements VulkanBuffer {

    protected final LogicalDevice<?> device;
    protected MemorySize size;
    private MemoryRegion memory;
    private Flag<BufferUsage> usage = BufferUsage.Storage;
    private boolean concurrent = false;

    public AbstractVulkanBuffer(LogicalDevice<?> device, MemorySize size) {
        this.device = device;
        this.size = size;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyBuffer(device.getNativeObject(), object, null);
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
        return memory.getFlags();
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Long getGpuObject() {
        return object;
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    protected MemoryRegion getMemory() {
        return memory;
    }

    protected abstract class Builder<T> extends AbstractNativeBuilder<T> {

        protected void construct(Flag<MemoryProp> memProps) {
            if (ref != null) {
                ref.destroy();
            }
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes())
                    .usage(getUsage().bits())
                    .sharingMode(VulkanUtils.sharingMode(isConcurrent()));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, idBuf),
                    "Failed to create buffer.");
            object = idBuf.get(0);
            createMemory(m -> m.setFlags(memProps));
            ref = DisposableManager.get().register(AbstractVulkanBuffer.this);
            device.getReference().addDependent(ref);
            getMemory().getReference().addDependent(ref);
        }

        protected void createMemory(Consumer<MemoryRegion.Builder> config) {
            memory = MemoryRegion.buildBufferMemory(stack, AbstractVulkanBuffer.this, config);
        }

        public void setUsage(Flag<BufferUsage> usage) {
            AbstractVulkanBuffer.this.usage = usage;
        }

        public void setConcurrent(boolean concurrent) {
            AbstractVulkanBuffer.this.concurrent = concurrent;
        }

    }

}
