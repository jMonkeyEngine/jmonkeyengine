package com.jme3.vulkan.formats;

import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;
import static com.jme3.vulkan.formats.GlComponent.*;

public enum Format {

    // todo: implement more formats

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

        Color(true, false, false, VulkanImage.Aspect.Color),
        Depth(false, true, false, VulkanImage.Aspect.Depth),
        DepthStencil(false, true, true, VulkanImage.Aspect.DepthStencil);

        private final boolean color, depth, stencil;
        private final Flag<VulkanImage.Aspect> imageAspect;

        Aspects(boolean color, boolean depth, boolean stencil, Flag<VulkanImage.Aspect> imageAspect) {
            this.color = color;
            this.depth = depth;
            this.stencil = stencil;
            this.imageAspect = imageAspect;
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

        public Flag<VulkanImage.Aspect> getImageAspect() {
            return imageAspect;
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

    public int getEnum(EnumInterpreter interpreter) {
        return interpreter.getFormatEnum(this);
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

    public static Format byEnum(EnumInterpreter interpreter, int enm) {
        for (Format f : values()) {
            if (f.getEnum(interpreter) == enm) return f;
        }
        throw new IllegalArgumentException("Format enum " + enm + " is not supported.");
    }

}
