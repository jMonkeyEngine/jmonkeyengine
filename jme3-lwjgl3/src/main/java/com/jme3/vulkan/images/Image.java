package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public interface Image extends Native<Long> {

    ImageView createView(VkImageViewCreateInfo create);

    LogicalDevice getDevice();

    int getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getFormat();

    default ImageView createView(int type, int aspects, int baseMip, int mipCount, int baseLayer, int layerCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo create = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(getNativeObject())
                    .viewType(type)
                    .format(getFormat());
            create.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            create.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(baseMip)
                    .levelCount(mipCount)
                    .baseArrayLayer(baseLayer)
                    .layerCount(layerCount);
            return createView(create);
        }
    }

}
