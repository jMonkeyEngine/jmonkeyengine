package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.buffers.MemoryRegion;
import com.jme3.vulkan.flags.ImageUsageFlags;
import com.jme3.vulkan.flags.MemoryFlags;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class GpuImage implements Image {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final long id;
    private final MemoryRegion memory;
    private final int type, width, height, depth;
    private final Image.Format format;
    private final Image.Tiling tiling;

    public GpuImage(LogicalDevice device, int width, int height, Image.Format format, Image.Tiling tiling, ImageUsageFlags usage, MemoryFlags mem) {
        this(device, VK_IMAGE_TYPE_2D, width, height, 1, format, tiling, usage, mem);
    }

    public GpuImage(LogicalDevice device, int type, int width, int height, int depth, Image.Format format, Image.Tiling tiling, ImageUsageFlags usage, MemoryFlags mem) {
        this.device = device;
        this.type = type;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.format = format;
        this.tiling = tiling;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo create = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(type)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(format.getVkEnum())
                    .tiling(tiling.getVkEnum())
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(usage.getUsageFlags())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            create.extent().width(width).height(height).depth(depth);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateImage(device.getNativeObject(), create, null, idBuf),
                    "Failed to create image.");
            id = idBuf.get(0);
            VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device.getNativeObject(), id, memReq);
            memory = new MemoryRegion(device, memReq.size(), device.getPhysicalDevice().findSupportedMemoryType(
                    stack, memReq.memoryTypeBits(), mem.getMemoryFlags()));
            memory.bind(this, 0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public ImageView createView(VkImageViewCreateInfo create) {
        return new ImageView(this, create);
    }

    @Override
    public LogicalDevice getDevice() {
        return device;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public Image.Format getFormat() {
        return format;
    }

    @Override
    public Image.Tiling getTiling() {
        return tiling;
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyImage(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void transitionLayout(CommandBuffer commands, Image.Layout srcLayout, Image.Layout dstLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] args = Image.Layout.getTransferArguments(srcLayout, dstLayout);
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(srcLayout.getVkEnum())
                    .newLayout(dstLayout.getVkEnum())
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // for transfering queue ownership
                    .image(id)
                    .srcAccessMask(args[0])
                    .dstAccessMask(args[1]);
            barrier.subresourceRange()
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            int aspect = 0;
            if (format.isColor()) {
                aspect |= VK_IMAGE_ASPECT_COLOR_BIT;
            }
            if (format.isDepth()) {
                aspect |= VK_IMAGE_ASPECT_DEPTH_BIT;
            }
            if (format.isStencil()) {
                aspect |= VK_IMAGE_ASPECT_STENCIL_BIT;
            }
            barrier.subresourceRange().aspectMask(aspect);
            vkCmdPipelineBarrier(commands.getBuffer(), args[2], args[3], 0, null, null, barrier);
        }
    }

}
