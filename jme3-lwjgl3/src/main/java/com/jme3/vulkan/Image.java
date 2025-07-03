package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Image implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final int width, height, format, tiling, usage, mem;
    protected LongBuffer id = MemoryUtil.memAllocLong(1);
    protected LongBuffer memory = MemoryUtil.memAllocLong(1);

    public Image(LogicalDevice device, int width, int height, int format, int tiling, int usage, int mem) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.format = format;
        this.tiling = tiling;
        this.usage = usage;
        this.mem = mem;
        VkImageCreateInfo create = VkImageCreateInfo.create()
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
        createImageMemory(create, mem);
        create.free();
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public Image(LogicalDevice device, VkImageCreateInfo create, int mem) {
        this.device = device;
        this.width = create.extent().width();
        this.height = create.extent().height();
        this.format = create.format();
        this.tiling = create.tiling();
        this.usage = create.usage();
        this.mem = mem;
        createImageMemory(create, mem);
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public Image(LogicalDevice device, long id) {
        width = height = format = tiling = usage = mem = 0; // todo: fix image interfacing
        this.device = device;
        this.id.put(0, id);
        this.memory.put(0, MemoryUtil.NULL);
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    private void createImageMemory(VkImageCreateInfo create, int mem) {
        create.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        vkCreateImage(device.getNativeObject(), create, null, id);
        VkMemoryRequirements memReq = VkMemoryRequirements.create();
        vkGetImageMemoryRequirements(device.getNativeObject(), id.get(0), memReq);
        VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReq.size())
                .memoryTypeIndex(device.getPhysicalDevice().findMemoryType(memReq.memoryTypeBits(), mem));
        check(vkAllocateMemory(device.getNativeObject(), allocate, null, memory), "Failed to allocate image memory");
        vkBindImageMemory(device.getNativeObject(), id.get(0), memory.get(0), 0);
        memReq.free();
        allocate.free();
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyImage(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
            MemoryUtil.memFree(memory);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
        memory = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public long getMemory() {
        return memory != null ? memory.get(0) : MemoryUtil.NULL;
    }

    public ImageView createView(VkImageViewCreateInfo create) {
        return new ImageView(this, create);
    }

}
