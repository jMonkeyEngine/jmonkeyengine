package com.jme3.shaderc;

import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;

public enum ShaderType {

    Vertex(VK10.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, VK10.VK_SHADER_STAGE_VERTEX_BIT, Shaderc.shaderc_vertex_shader),
    Fragment(VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK10.VK_SHADER_STAGE_FRAGMENT_BIT, Shaderc.shaderc_fragment_shader),
    Tessellation(VK10.VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT, VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT, Shaderc.shaderc_tess_evaluation_shader),
    TessellationControl(VK10.VK_PIPELINE_STAGE_TESSELLATION_CONTROL_SHADER_BIT, VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT, Shaderc.shaderc_tess_control_shader),
    Geometry(VK10.VK_PIPELINE_STAGE_GEOMETRY_SHADER_BIT, VK10.VK_SHADER_STAGE_GEOMETRY_BIT, Shaderc.shaderc_geometry_shader),
    Compute(VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK10.VK_SHADER_STAGE_COMPUTE_BIT, Shaderc.shaderc_compute_shader);

    private final int vulkanPipeline;
    private final int vulkanShader;
    private final int shaderc;

    ShaderType(int vulkanPipeline, int vulkanShader, int shaderc) {
        this.vulkanPipeline = vulkanPipeline;
        this.vulkanShader = vulkanShader;
        this.shaderc = shaderc;
    }

    public int getVulkanPipeline() {
        return vulkanPipeline;
    }

    public int getVulkanShader() {
        return vulkanShader;
    }

    public int getShaderc() {
        return shaderc;
    }

}
