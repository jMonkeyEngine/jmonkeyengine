package com.jme3.vulkan.buffers;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.BufferOverflowException;
import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class BasicVulkanBuffer extends AbstractNative<Long> implements VulkanBuffer {

    private final LogicalDevice<?> device;
    private final MemorySize size;
    protected final MemoryRegion memory;

    public BasicVulkanBuffer(LogicalDevice<?> device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryProp> mem, boolean concurrent) {
        this.device = device;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes())
                    .usage(usage.bits())
                    .sharingMode(VulkanUtils.sharingMode(concurrent));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, idBuf),
                    "Failed to create buffer.");
            object = idBuf.get(0);
            VkMemoryRequirements bufferMem = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getNativeObject(), object, bufferMem);
            memory = new MemoryRegion(device, bufferMem.size(), mem, bufferMem.memoryTypeBits());
            memory.bind(this, 0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
        memory.getNativeReference().addDependent(ref);
    }

    @Override
    public long getId() {
        return object;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyBuffer(device.getNativeObject(), object, null);
    }

    @Override
    public void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size.getBytes()) {
            throw new BufferOverflowException();
        }
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return memory.map(offset, size);
    }

    @Override
    public void unmap() {
        memory.unmap();
    }

    @Override
    public void freeMemory() {
        memory.getNativeReference().destroy();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return device;
    }

}
