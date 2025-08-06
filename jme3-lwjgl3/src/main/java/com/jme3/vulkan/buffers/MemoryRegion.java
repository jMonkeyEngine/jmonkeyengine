package com.jme3.vulkan.buffers;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.Image;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryRegion implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final long id;
    private final long size;
    private final AtomicBoolean mapped = new AtomicBoolean(false);

    public MemoryRegion(LogicalDevice<?> device, long size, int typeIndex) {
        this.device = device;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(size)
                    .memoryTypeIndex(typeIndex);
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
        return () -> vkFreeMemory(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void bind(GpuBuffer buffer, long offset) {
        check(vkBindBufferMemory(device.getNativeObject(), buffer.getNativeObject(), id, offset),
                "Failed to bind buffer memory.");
    }

    public void bind(Image image, long offset) {
        check(vkBindImageMemory(device.getNativeObject(), image.getNativeObject(), id, offset),
                "Failed to bind image memory.");
    }

    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        if (mapped.getAndSet(true)) {
            throw new IllegalStateException("Memory already mapped.");
        }
        PointerBuffer data = stack.mallocPointer(1);
        vkMapMemory(device.getNativeObject(), id, offset, size, flags, data);
        return data;
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory is not mapped.");
        }
        vkUnmapMemory(device.getNativeObject(), id);
    }

    public long getSize() {
        return size;
    }

}
