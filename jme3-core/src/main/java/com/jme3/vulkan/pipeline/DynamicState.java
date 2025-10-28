package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum DynamicState implements IntEnum<DynamicState> {

    ViewPort(VK_DYNAMIC_STATE_VIEWPORT),
    Scissor(VK_DYNAMIC_STATE_SCISSOR),
    LineWidth(VK_DYNAMIC_STATE_LINE_WIDTH),
    DepthBias(VK_DYNAMIC_STATE_DEPTH_BIAS),
    BlendConstants(VK_DYNAMIC_STATE_BLEND_CONSTANTS),
    DepthBounds(VK_DYNAMIC_STATE_DEPTH_BOUNDS),
    CompareMask(VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK),
    WriteMask(VK_DYNAMIC_STATE_STENCIL_WRITE_MASK),
    StencilReference(VK_DYNAMIC_STATE_STENCIL_REFERENCE);

    private final int vk;

    DynamicState(int vk) {
        this.vk = vk;
    }

    @Override
    public int getEnum() {
        return vk;
    }

}
