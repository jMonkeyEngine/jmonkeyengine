package com.jme3.vulkan.images;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum ImageUsage implements Flag<ImageUsage> {

    TransferDst(VK_IMAGE_USAGE_TRANSFER_DST_BIT),
    TransferSrc(VK_IMAGE_USAGE_TRANSFER_SRC_BIT),
    ColorAttachment(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
    Sampled(VK_IMAGE_USAGE_SAMPLED_BIT),
    Storage(VK_IMAGE_USAGE_STORAGE_BIT),
    DepthStencilAttachment(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
    InputAttachment(VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT),
    TransientAttachment(VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT);

    private final int bits;

    ImageUsage(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }

}
