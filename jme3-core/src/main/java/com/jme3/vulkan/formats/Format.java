package com.jme3.vulkan.formats;

import com.jme3.vulkan.util.AdaptiveEnum;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public class Format implements AdaptiveEnum<Format> {

    public static final int GL_UNKNOWN = -1;
    public static final int GL_DOUBLE = 5130;
    public static final int GL_FLOAT = 5126;
    public static final int GL_INT = 5124;
    public static final int GL_UINT = 5125;
    public static final int GL_SHORT = 5122;
    public static final int GL_USHORT = 5123;
    public static final int GL_BYTE = 5120;
    public static final int GL_UBYTE = 5121;

    public static final Format

    RGBA32_SFloat = new Format("RGBA32_SFloat", GL_FLOAT, 16, 4, ComponentFormat.SFloat, Aspects.Color),
    RGB32_SFloat = new Format("RGB32_SFloat", GL_FLOAT, 12, 3, ComponentFormat.SFloat, Aspects.Color),
    RG32_SFloat = new Format("RG32_SFloat", GL_FLOAT, 8, 2, ComponentFormat.SFloat, Aspects.Color),
    R32_SFloat = new Format("R32_SFloat", GL_FLOAT, 4, 1, ComponentFormat.SFloat, Aspects.Color),

    RGBA8_SRGB = new Format("RGBA8_SRGB", GL_BYTE, 4, 4, ComponentFormat.SRGB, Aspects.Color),
    R8_SRGB = new Format("R8_SRGB", GL_BYTE, 1, 1, ComponentFormat.SRGB, Aspects.Color),
    BGR8_SRGB = new Format("BGR8_SRGB", GL_BYTE, 3, 3, ComponentFormat.SRGB, Aspects.Color),
    ABGR8_SRGB_Pack32 = new Format("ABGR8_SRGB", GL_BYTE, 4, 1, ComponentFormat.SRGB, Aspects.Color),
    BGRA8_SRGB = new Format("BGRA8_SRGB", GL_BYTE, 4, 4, ComponentFormat.SRGB, Aspects.Color),

    Depth32_SFloat = new Format("Depth32_SFloat", GL_FLOAT, 4, 1, ComponentFormat.SFloat, Aspects.Depth),
    Depth32_SFloat_Stencil8_UInt = new Format("Depth32_SFloat_Stencil8_UInt", 5, 2, Aspects.DepthStencil),
    Depth24_UNorm_Stencil8_UInt = new Format("Depth24_UNorm_Stencil8_UInt", 4, 2, Aspects.DepthStencil),
    Depth16_UNorm = new Format("Depth16_UNorm", GL_USHORT, 2, 1, ComponentFormat.UNorm, Aspects.Depth),
    Depth16_UNorm_Stencil8_UInt = new Format("Depth16_UNorm_Stencil8_UInt", 3, 2, Aspects.DepthStencil);

    public enum ComponentFormat {

        Undefined(false),
        SFloat(false),
        SNorm(true),
        UNorm(true),
        UInt(false),
        SRGB(true);

        private final boolean normalized;

        ComponentFormat(boolean normalized) {
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

    private final String name;
    private final int glBufferCompFormat;
    private final int bytes;
    private final int components;
    private final ComponentFormat compFmt;
    private final Aspects aspects;
    private int enumVal = -1;

    public Format(String name, int bytes, int components, Aspects aspects) {
        this(name, GL_UNKNOWN, bytes, components, ComponentFormat.Undefined, aspects);
    }

    public Format(String name, int glBufferCompFormat, int bytes, int components, ComponentFormat compFmt, Aspects aspects) {
        this.name = name;
        this.glBufferCompFormat = glBufferCompFormat;
        this.bytes = bytes;
        this.components = components;
        this.compFmt = compFmt;
        this.aspects = aspects;
    }

    @Override
    public int getEnum() {
        if (enumVal < 0) {
            throw new IllegalStateException(name + " is not supported or not configured.");
        }
        return enumVal;
    }

    @Override
    public Format set(int enumVal) {
        this.enumVal = enumVal;
        return this;
    }

    public int getGlBufferComponentType() {
        return glBufferCompFormat;
    }

    public int getBytes() {
        return bytes;
    }

    public int getComponents() {
        return components;
    }

    public ComponentFormat getComponentFormat() {
        return compFmt;
    }

    public Aspects getAspects() {
        return aspects;
    }

    public boolean isSupported() {
        return enumVal >= 0;
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
