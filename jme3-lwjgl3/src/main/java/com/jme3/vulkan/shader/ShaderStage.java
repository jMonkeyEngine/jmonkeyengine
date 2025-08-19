package com.jme3.vulkan.shader;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum ShaderStage implements Flag<ShaderStage> {

    All(VK_SHADER_STAGE_ALL),
    AllGraphics(VK_SHADER_STAGE_ALL_GRAPHICS),
    Vertex(VK_SHADER_STAGE_VERTEX_BIT),
    Geometry(VK_SHADER_STAGE_GEOMETRY_BIT),
    TessellationEval(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT),
    TessellationControl(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT),
    Fragment(VK_SHADER_STAGE_FRAGMENT_BIT),
    Compute(VK_SHADER_STAGE_COMPUTE_BIT);

    private final int vkEnum;

    ShaderStage(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int bits() {
        return vkEnum;
    }

}
