package com.jme3.vulkan.pipelines.states;

import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

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

}
