package com.jme3.vulkan.memory;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryRegion implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final Flag<MemoryProp> flags;
    private final long id;
    private final long size;
    private final AtomicBoolean mapped = new AtomicBoolean(false);
    private final PointerBuffer mapping = MemoryUtil.memCallocPointer(1);

    public MemoryRegion(LogicalDevice<?> device, long size, Flag<MemoryProp> flags, int typeBits) {
        this.device = device;
        this.flags = flags;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(size)
                    .memoryTypeIndex(device.getPhysicalDevice().findSupportedMemoryType(stack, typeBits, flags));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkAllocateMemory(device.getNativeObject(), allocate, null, idBuf),
                    "Failed to allocate buffer memory.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkFreeMemory(device.getNativeObject(), id, null);
            MemoryUtil.memFree(mapping);
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void bind(GpuBuffer buffer, long offset) {
        check(vkBindBufferMemory(device.getNativeObject(), buffer.getId(), id, offset),
                "Failed to bind buffer memory.");
    }

    public void bind(Image image, long offset) {
        check(vkBindImageMemory(device.getNativeObject(), image.getNativeObject(), id, offset),
                "Failed to bind image memory.");
    }

    public PointerBuffer map() {
        return map(0L, VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset) {
        return map(offset, VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset, long size) {
        if (mapped.getAndSet(true)) {
            throw new IllegalStateException("Memory already mapped.");
        }
        if (!this.flags.contains(MemoryProp.HostVisible)) {
            throw new IllegalStateException("Cannot map memory that is not host visible.");
        }
        vkMapMemory(device.getNativeObject(), id, offset, size, 0, mapping);
        return mapping;
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory is not mapped.");
        }
        mapping.put(0, VK_NULL_HANDLE);
        vkUnmapMemory(device.getNativeObject(), id);
    }

    public long getSize() {
        return size;
    }

}
