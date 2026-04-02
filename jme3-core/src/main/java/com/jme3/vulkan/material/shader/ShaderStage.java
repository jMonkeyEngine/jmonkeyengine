package com.jme3.vulkan.material.shader;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.util.shaderc.Shaderc.*;

public enum ShaderStage implements Flag<ShaderStage> {

    All(VkEnums.VK_SHADER_STAGE_ALL, -1),
    AllGraphics(VkEnums.VK_SHADER_STAGE_ALL_GRAPHICS, -1),
    Vertex(VkEnums.VK_SHADER_STAGE_VERTEX_BIT, shaderc_vertex_shader),
    Geometry(VkEnums.VK_SHADER_STAGE_GEOMETRY_BIT, shaderc_geometry_shader),
    TessellationEval(VkEnums.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT, shaderc_tess_evaluation_shader),
    TessellationControl(VkEnums.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT, shaderc_tess_control_shader),
    Fragment(VkEnums.VK_SHADER_STAGE_FRAGMENT_BIT, shaderc_fragment_shader),
    Compute(VkEnums.VK_SHADER_STAGE_COMPUTE_BIT, shaderc_compute_shader);

    private final int vk;
    private final int shaderc;

    ShaderStage(int vk, int shaderc) {
        this.vk = vk;
        this.shaderc = shaderc;
    }

    @Override
    public int bits() {
        return vk;
    }

    @Override
    public boolean is(int bits) {
        return Flag.super.is(bits);
    }

    public int getVk() {
        return vk;
    }

    public int getShaderc() {
        return shaderc;
    }

    private static class VkEnums {

        public static final int VK_SHADER_STAGE_VERTEX_BIT = 1;
        public static final int VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT = 2;
        public static final int VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT = 4;
        public static final int VK_SHADER_STAGE_GEOMETRY_BIT = 8;
        public static final int VK_SHADER_STAGE_FRAGMENT_BIT = 16;
        public static final int VK_SHADER_STAGE_COMPUTE_BIT = 32;
        public static final int VK_SHADER_STAGE_ALL_GRAPHICS = 31;
        public static final int VK_SHADER_STAGE_ALL = 2147483647;

    }

}
