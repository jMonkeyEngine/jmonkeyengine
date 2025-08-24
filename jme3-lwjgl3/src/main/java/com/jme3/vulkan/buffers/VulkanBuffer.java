package com.jme3.vulkan.buffers;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryFlag;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.*;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanBuffer implements GpuBuffer, Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final MemorySize size;
    private final long id;
    protected final MemoryRegion memory;

    public VulkanBuffer(LogicalDevice<?> device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryFlag> mem, boolean concurrent) {
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
            id = idBuf.get(0);
            VkMemoryRequirements bufferMem = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getNativeObject(), id, bufferMem);
            memory = new MemoryRegion(device, bufferMem.size(), device.getPhysicalDevice().findSupportedMemoryType(
                    stack, bufferMem.memoryTypeBits(), mem));
            memory.bind(this, 0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
        memory.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyBuffer(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    @Override
    public void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size.getBytes()) {
            throw new BufferOverflowException();
        }
    }

    @Override
    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        return memory.map(stack, offset, size, flags);
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

    public LogicalDevice<?> getDevice() {
        return device;
    }

}
