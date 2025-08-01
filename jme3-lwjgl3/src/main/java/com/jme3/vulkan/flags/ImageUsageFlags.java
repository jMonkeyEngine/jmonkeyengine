package com.jme3.vulkan.flags;

import static org.lwjgl.vulkan.VK10.*;

public class ImageUsageFlags {

    private int usageFlags;

    public ImageUsageFlags transferSrc() {
        usageFlags |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        return this;
    }

    public ImageUsageFlags transferDst() {
        usageFlags |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
        return this;
    }

    public ImageUsageFlags sampled() {
        usageFlags |= VK_IMAGE_USAGE_SAMPLED_BIT;
        return this;
    }

    public int getUsageFlags() {
        return usageFlags;
    }

}
