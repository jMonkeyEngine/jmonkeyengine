package com.jme3.vulkan.formats;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;
import static com.jme3.vulkan.formats.GlComponent.*;

public enum Format {

    RGBA32_SFloat(16, 4, Space.SFloat, Aspects.Color, GL_FLOAT),
    RGB32_SFloat(12, 3, Space.SFloat, Aspects.Color, GL_FLOAT),
    RG32_SFloat(8, 2, Space.SFloat, Aspects.Color, GL_FLOAT),
    R32_SFloat(4, 1, Space.SFloat, Aspects.Color, GL_FLOAT),

    RGBA8_SRGB(4, 4, Space.SRGB, Aspects.Color, GL_UBYTE),
    R8_SRGB(1, 1, Space.SRGB, Aspects.Color, GL_UBYTE),
    BGR8_SRGB(3, 3, Space.SRGB, Aspects.Color, GL_UBYTE),
    ABGR8_SRGB_Pack32(4, 1, Space.SRGB, Aspects.Color, GL_UBYTE),
    BGRA8_SRGB(4, 4, Space.SRGB, Aspects.Color, GL_UBYTE),

    Depth32_SFloat(4, 1, Space.SFloat, Aspects.Depth, GL_FLOAT),
    Depth32_SFloat_Stencil8_UInt(5, 2, Space.SFloat, Aspects.DepthStencil, GL_UNKNOWN),
    Depth24_UNorm_Stencil8_UInt(4, 2, Space.UNorm, Aspects.DepthStencil, GL_UNKNOWN),
    Depth16_UNorm(2, 1, Space.UNorm, Aspects.Depth, GL_USHORT),
    Depth16_UNorm_Stencil8_UInt(3, 2, Space.UNorm, Aspects.DepthStencil, GL_UNKNOWN);

    public enum Space {

        Undefined(false),
        SFloat(false),
        SNorm(true),
        UNorm(true),
        UInt(false),
        SRGB(true);

        private final boolean normalized;

        Space(boolean normalized) {
            this.normalized = normalized;
        }

        public boolean isNormalized() {
            return normalized;
        }

    }

    public enum Aspects {

        Color(true, false, false),
        Depth(false, true, false),
        DepthStencil(false, true, true);

        private final boolean color, depth, stencil;

        Aspects(boolean color, boolean depth, boolean stencil) {
            this.color = color;
            this.depth = depth;
            this.stencil = stencil;
        }

        public boolean isColor() {
            return color;
        }

        public boolean isDepth() {
            return depth;
        }

        public boolean isStencil() {
            return stencil;
        }

    }

    private final int bytes;
    private final int components;
    private final Space space;
    private final Aspects aspects;
    private final int glComponent;

    Format(int bytes, int components, Space space, Aspects aspects, int glComponent) {
        this.bytes = bytes;
        this.components = components;
        this.space = space;
        this.aspects = aspects;
        this.glComponent = glComponent;
    }

    public int getEnum(FormatInterpreter interpreter) {
        return interpreter.getEnum(this);
    }

    public int getBytes() {
        return bytes;
    }

    public int getComponents() {
        return components;
    }

    public Space getSpace() {
        return space;
    }

    public Aspects getAspects() {
        return aspects;
    }

    public int getGlComponent() {
        return glComponent;
    }

    public enum Feature implements Flag<Feature> {

        DepthStencilAttachment(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT),
        BlitDst(VK_FORMAT_FEATURE_BLIT_DST_BIT),
        BlitSrc(VK_FORMAT_FEATURE_BLIT_SRC_BIT),
        ColorAttachment(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
        SampledImage(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
        ColorAttachmentBlend(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
        SampledImageFilterLinear(VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT),
        StorageImageAtomic(VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT),
        StorageImage(VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT),
        StorageTexelBufferAtomic(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT),
        StorageTexelBuffer(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT),
        UniformTexelBuffer(VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT),
        VertexBuffer(VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);

        private final int bits;

        Feature(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

}
