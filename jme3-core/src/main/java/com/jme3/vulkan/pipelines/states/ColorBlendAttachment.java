package com.jme3.vulkan.pipelines.states;

import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class ColorBlendAttachment {

    private int colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                               | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    private boolean blend = false;
    private int srcColorBlendFactor = VK_BLEND_FACTOR_ONE;
    private int dstColorBlendFactor = VK_BLEND_FACTOR_ZERO;
    private int colorBlendOp = VK_BLEND_OP_ADD;
    private int srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
    private int dstAlphaBlendFactor = VK_BLEND_FACTOR_ZERO;
    private int alphaBlendOp = VK_BLEND_OP_ADD;
    protected long version = 0L;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColorBlendAttachment that = (ColorBlendAttachment) o;
        return colorWriteMask == that.colorWriteMask
                && blend == that.blend
                && srcColorBlendFactor == that.srcColorBlendFactor
                && dstColorBlendFactor == that.dstColorBlendFactor
                && colorBlendOp == that.colorBlendOp
                && srcAlphaBlendFactor == that.srcAlphaBlendFactor
                && dstAlphaBlendFactor == that.dstAlphaBlendFactor
                && alphaBlendOp == that.alphaBlendOp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorWriteMask, blend, srcColorBlendFactor, dstColorBlendFactor, colorBlendOp,
                srcAlphaBlendFactor, dstAlphaBlendFactor, alphaBlendOp);
    }

    public long getCurrentVersion() {
        return version;
    }

    public void writeToStruct(VkPipelineColorBlendAttachmentState struct) {
        struct.colorWriteMask(colorWriteMask)
                .blendEnable(blend)
                .srcColorBlendFactor(srcColorBlendFactor)
                .dstColorBlendFactor(dstColorBlendFactor)
                .colorBlendOp(colorBlendOp)
                .srcAlphaBlendFactor(srcAlphaBlendFactor)
                .dstAlphaBlendFactor(dstAlphaBlendFactor)
                .alphaBlendOp(alphaBlendOp);
    }

    public ColorBlendAttachment copy() {
        ColorBlendAttachment copy = new ColorBlendAttachment();
        copy.colorWriteMask = colorWriteMask;
        copy.blend = blend;
        copy.srcColorBlendFactor = srcColorBlendFactor;
        copy.dstColorBlendFactor = dstColorBlendFactor;
        copy.colorBlendOp = colorBlendOp;
        copy.srcAlphaBlendFactor = srcAlphaBlendFactor;
        copy.dstAlphaBlendFactor = dstAlphaBlendFactor;
        copy.alphaBlendOp = alphaBlendOp;
        return copy;
    }

    public void setColorWriteMask(int colorWriteMask) {
        if (this.colorWriteMask != colorWriteMask) {
            this.colorWriteMask = colorWriteMask;
            version++;
        }
    }

    public void setBlend(boolean blend) {
        if (this.blend != blend) {
            this.blend = blend;
            version++;
        }
    }

    public void setSrcColorBlendFactor(int srcColorBlendFactor) {
        if (this.srcColorBlendFactor != srcColorBlendFactor) {
            this.srcColorBlendFactor = srcColorBlendFactor;
            version++;
        }
    }

    public void setDstColorBlendFactor(int dstColorBlendFactor) {
        if (this.dstColorBlendFactor != dstColorBlendFactor) {
            this.dstColorBlendFactor = dstColorBlendFactor;
            version++;
        }
    }

    public void setColorBlendOp(int colorBlendOp) {
        if (this.colorBlendOp != colorBlendOp) {
            this.colorBlendOp = colorBlendOp;
            version++;
        }
    }

    public void setSrcAlphaBlendFactor(int srcAlphaBlendFactor) {
        if (this.srcAlphaBlendFactor != srcAlphaBlendFactor) {
            this.srcAlphaBlendFactor = srcAlphaBlendFactor;
            version++;
        }
    }

    public void setDstAlphaBlendFactor(int dstAlphaBlendFactor) {
        if (this.dstAlphaBlendFactor != dstAlphaBlendFactor) {
            this.dstAlphaBlendFactor = dstAlphaBlendFactor;
            version++;
        }
    }

    public void setAlphaBlendOp(int alphaBlendOp) {
        if (this.alphaBlendOp != alphaBlendOp) {
            this.alphaBlendOp = alphaBlendOp;
            version++;
        }
    }

    public int getColorWriteMask() {
        return colorWriteMask;
    }

    public boolean isBlend() {
        return blend;
    }

    public int getSrcColorBlendFactor() {
        return srcColorBlendFactor;
    }

    public int getDstColorBlendFactor() {
        return dstColorBlendFactor;
    }

    public int getColorBlendOp() {
        return colorBlendOp;
    }

    public int getSrcAlphaBlendFactor() {
        return srcAlphaBlendFactor;
    }

    public int getDstAlphaBlendFactor() {
        return dstAlphaBlendFactor;
    }

    public int getAlphaBlendOp() {
        return alphaBlendOp;
    }

}
