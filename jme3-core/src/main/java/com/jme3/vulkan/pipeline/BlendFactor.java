package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum BlendFactor implements IntEnum<BlendFactor> {

    Zero(VK_BLEND_FACTOR_ZERO),
    One(VK_BLEND_FACTOR_ONE),
    ConstantColor(VK_BLEND_FACTOR_CONSTANT_COLOR),
    ConstantAlpha(VK_BLEND_FACTOR_CONSTANT_ALPHA),
    DstColor(VK_BLEND_FACTOR_DST_COLOR),
    DstAlpha(VK_BLEND_FACTOR_DST_ALPHA),
    OneMinusConstantColor(VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR),
    OneMinusConstantAlpha(VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA),
    OneMinusDstColor(VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR),
    OneMinusDstAlpha(VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA),
    OneMinusSrcColor(VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR),
    OneMinusSrcAlpha(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA),
    OneMinusSrc1Color(VK_BLEND_FACTOR_ONE_MINUS_SRC1_COLOR),
    OneMinusSrc1Alpha(VK_BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA),
    SrcColor(VK_BLEND_FACTOR_SRC_COLOR),
    SrcAlpha(VK_BLEND_FACTOR_SRC_ALPHA),
    Src1Color(VK_BLEND_FACTOR_SRC1_COLOR),
    Src1Alpha(VK_BLEND_FACTOR_SRC1_ALPHA),
    SrcAlphaSaturate(VK_BLEND_FACTOR_SRC_ALPHA_SATURATE);

    private final int vk;

    BlendFactor(int vk) {
        this.vk = vk;
    }

    @Override
    public int getEnum() {
        return vk;
    }

}
