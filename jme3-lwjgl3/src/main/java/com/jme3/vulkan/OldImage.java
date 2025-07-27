package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class OldImage implements Image {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final int width, height, format, tiling, usage, mem;
    private long id, memory;

    public OldImage(LogicalDevice device, int width, int height, int format, int tiling, int usage, int mem) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.format = format;
        this.tiling = tiling;
        this.usage = usage;
        this.mem = mem;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo create = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(format)
                    .tiling(tiling)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(usage)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            create.extent().width(width).height(height).depth(1);
            createImageMemory(stack, create, mem);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public OldImage(LogicalDevice device, VkImageCreateInfo create, int mem) {
        this.device = device;
        this.width = create.extent().width();
        this.height = create.extent().height();
        this.format = create.format();
        this.tiling = create.tiling();
        this.usage = create.usage();
        this.mem = mem;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            createImageMemory(stack, create, mem);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public OldImage(LogicalDevice device, long id) {
        width = height = format = tiling = usage = mem = 0; // todo: fix image interfacing
        this.device = device;
        System.out.println("Assign image ID: " + id);
        this.id = id;
        this.memory = VK_NULL_HANDLE;
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    private void createImageMemory(MemoryStack stack, VkImageCreateInfo create, int mem) {
        create.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        vkCreateImage(device.getNativeObject(), create, null, stack.longs(id));
        VkMemoryRequirements memReq = VkMemoryRequirements.create();
        vkGetImageMemoryRequirements(device.getNativeObject(), id, memReq);
        VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(device.getPhysicalDevice().findMemoryType(memReq.memoryTypeBits(), mem));
        LongBuffer memBuf = stack.mallocLong(1);
        check(vkAllocateMemory(device.getNativeObject(), allocate, null, memBuf), "Failed to allocate image memory");
        memory = memBuf.get(0);
        vkBindImageMemory(device.getNativeObject(), id, memory, 0);
        memReq.free();
        allocate.free();
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyImage(device.getNativeObject(), id, null);
            vkFreeMemory(device.getNativeObject(), memory, null);
            id = VK_NULL_HANDLE;
            memory = VK_NULL_HANDLE;
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    @Override
    public LogicalDevice getDevice() {
        return device;
    }

    public long getMemory() {
        return memory;
    }

    @Override
    public ImageView createView(VkImageViewCreateInfo create) {
        return new ImageView(this, create);
    }

}
