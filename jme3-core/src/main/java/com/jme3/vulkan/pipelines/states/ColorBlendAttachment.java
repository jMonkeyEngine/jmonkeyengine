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
        this.colorWriteMask = colorWriteMask;
    }

    public void setBlend(boolean blend) {
        this.blend = blend;
    }

    public void setSrcColorBlendFactor(int srcColorBlendFactor) {
        this.srcColorBlendFactor = srcColorBlendFactor;
    }

    public void setDstColorBlendFactor(int dstColorBlendFactor) {
        this.dstColorBlendFactor = dstColorBlendFactor;
    }

    public void setColorBlendOp(int colorBlendOp) {
        this.colorBlendOp = colorBlendOp;
    }

    public void setSrcAlphaBlendFactor(int srcAlphaBlendFactor) {
        this.srcAlphaBlendFactor = srcAlphaBlendFactor;
    }

    public void setDstAlphaBlendFactor(int dstAlphaBlendFactor) {
        this.dstAlphaBlendFactor = dstAlphaBlendFactor;
    }

    public void setAlphaBlendOp(int alphaBlendOp) {
        this.alphaBlendOp = alphaBlendOp;
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
