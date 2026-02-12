package com.jme3.vulkan.memory;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryRegion extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final long size;
    private final AtomicBoolean mapped = new AtomicBoolean(false);
    private final PointerBuffer mapping = MemoryUtil.memCallocPointer(1);
    private Flag<MemoryProp> flags = MemoryProp.DeviceLocal;
    private int type;

    protected MemoryRegion(LogicalDevice<?> device, long size) {
        this.device = device;
        this.size = size;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> {
            vkFreeMemory(device.getNativeObject(), object, null);
            MemoryUtil.memFree(mapping);
        };
    }

    public void bind(VulkanBuffer buffer, long offset) {
        check(vkBindBufferMemory(device.getNativeObject(), buffer.getGpuObject(), object, offset),
                "Failed to bind buffer memory.");
    }

    public void bind(VulkanImage image, long offset) {
        check(vkBindImageMemory(device.getNativeObject(), image.getId(), object, offset),
                "Failed to bind image memory.");
    }

    public PointerBuffer map() {
        return map(0L, VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset) {
        return map(offset, VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset, long size) {
        if (!flags.contains(MemoryProp.HostVisible)) {
            throw new IllegalStateException("Cannot map memory that is not host visible.");
        }
        if (mapped.getAndSet(true)) {
            throw new IllegalStateException("Memory already mapped.");
        }
        vkMapMemory(device.getNativeObject(), object, offset, size, 0, mapping);
        return mapping;
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory not mapped.");
        }
        mapping.put(0, VK_NULL_HANDLE);
        vkUnmapMemory(device.getNativeObject(), object);
    }

    public long getSize() {
        return size;
    }

    public Flag<MemoryProp> getFlags() {
        return flags;
    }

    public int getType() {
        return type;
    }

    public static MemoryRegion build(LogicalDevice<?> device, long size, Consumer<Builder> config) {
        Builder b = new MemoryRegion(device, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public static MemoryRegion buildBufferMemory(VulkanBuffer buffer, Consumer<Builder> config) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return buildBufferMemory(stack, buffer, config);
        }
    }

    public static MemoryRegion buildBufferMemory(MemoryStack stack, VulkanBuffer buffer, Consumer<Builder> config) {
        VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
        vkGetBufferMemoryRequirements(buffer.getDevice().getNativeObject(), buffer.getGpuObject(), memReq);
        MemoryRegion mem = build(buffer.getDevice(), buffer.size().getBytes(), b -> {
            b.setUsableMemoryTypes(memReq.memoryTypeBits());
            config.accept(b);
        });
        mem.bind(buffer, 0);
        return mem;
    }

    public class Builder extends AbstractNativeBuilder<MemoryRegion> {

        private int usableMemoryTypes = 0;

        @Override
        protected MemoryRegion construct() {
            if (usableMemoryTypes == 0) {
                throw new IllegalStateException("No usable memory types specified.");
            }
            type = device.getPhysicalDevice().findSupportedMemoryType(stack, usableMemoryTypes, flags);
            VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(size)
                    .memoryTypeIndex(type);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkAllocateMemory(device.getNativeObject(), allocate, null, idBuf),
                    "Failed to allocate buffer memory.");
            object = idBuf.get(0);
            ref = DisposableManager.reference(MemoryRegion.this);
            device.getReference().addDependent(ref);
            return MemoryRegion.this;
        }

        public void setFlags(Flag<MemoryProp> flags) {
            MemoryRegion.this.flags = flags;
        }

        public void setUsableMemoryTypes(int usableMemoryTypes) {
            this.usableMemoryTypes = usableMemoryTypes;
        }

        public int getUsableMemoryTypes() {
            return usableMemoryTypes;
        }

    }

}
