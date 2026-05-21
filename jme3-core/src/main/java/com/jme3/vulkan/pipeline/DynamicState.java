package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK14.*;

public enum DynamicState implements IntEnum<DynamicState> {

    ViewPort(VK_DYNAMIC_STATE_VIEWPORT),
    Scissor(VK_DYNAMIC_STATE_SCISSOR),
    LineWidth(VK_DYNAMIC_STATE_LINE_WIDTH),
    DepthBias(VK_DYNAMIC_STATE_DEPTH_BIAS),
    BlendConstants(VK_DYNAMIC_STATE_BLEND_CONSTANTS),
    DepthBounds(VK_DYNAMIC_STATE_DEPTH_BOUNDS),
    DepthBoundsTest(VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE),
    CompareMask(VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK),
    WriteMask(VK_DYNAMIC_STATE_STENCIL_WRITE_MASK),
    StencilReference(VK_DYNAMIC_STATE_STENCIL_REFERENCE),
    CullMode(VK_DYNAMIC_STATE_CULL_MODE),
    PrimitiveRestart(VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE),
    DepthTest(VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE),
    DepthWrite(VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE),
    RasterizerDiscard(VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE),
    StencilTest(VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE),
    DepthCompare(VK_DYNAMIC_STATE_DEPTH_COMPARE_OP),
    DepthBiasEnabled(VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE),
    Topology(VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY);

    private final int vk;

    DynamicState(int vk) {
        this.vk = vk;
    }

    @Override
    public int getEnum() {
        return vk;
    }

}
