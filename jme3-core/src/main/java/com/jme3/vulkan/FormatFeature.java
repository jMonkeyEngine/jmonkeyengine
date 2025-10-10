package com.jme3.vulkan;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum FormatFeature implements Flag<FormatFeature> {

    DepthStencilAttachment(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT),
    BlitDst(VK_FORMAT_FEATURE_BLIT_DST_BIT),
    BlitSrc(VK_FORMAT_FEATURE_BLIT_SRC_BIT),
    ColorAttachment(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
    ColorAttachmentBlend(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
    SampledImage(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
    SampledImageFilterLinear(VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT),
    StorageImageAtomic(VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT),
    StorageImage(VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT),
    StorageTexelBufferAtomic(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT),
    StorageTexelBuffer(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT),
    UniformTexelBuffer(VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT),
    VertexBuffer(VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);

    private final int bits;

    FormatFeature(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return 0;
    }

}
