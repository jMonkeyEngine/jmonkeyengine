package com.jme3.vulkan.material.shader;

import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.util.Objects;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class ShaderStage implements Flag<ShaderStage>, IntEnum<ShaderStage> {

    public static final ShaderStage All = new ShaderStage(VK_SHADER_STAGE_ALL, -1);
    public static final ShaderStage AllGraphics = new ShaderStage(VK_SHADER_STAGE_ALL_GRAPHICS, -1);
    public static final ShaderStage Vertex = new ShaderStage(VK_SHADER_STAGE_VERTEX_BIT, shaderc_vertex_shader);
    public static final ShaderStage Geometry = new ShaderStage(VK_SHADER_STAGE_GEOMETRY_BIT, shaderc_geometry_shader);
    public static final ShaderStage TessellationEval = new ShaderStage(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT, shaderc_tess_evaluation_shader);
    public static final ShaderStage TessellationControl = new ShaderStage(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT, shaderc_tess_control_shader);
    public static final ShaderStage Fragment = new ShaderStage(VK_SHADER_STAGE_FRAGMENT_BIT, shaderc_fragment_shader);
    public static final ShaderStage Compute = new ShaderStage(VK_SHADER_STAGE_COMPUTE_BIT, shaderc_compute_shader);

    private final int vk;
    private final int shaderc;

    public ShaderStage(int vk, int shaderc) {
        this.vk = vk;
        this.shaderc = shaderc;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderStage integers = (ShaderStage) o;
        return vk == integers.vk && shaderc == integers.shaderc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vk, shaderc);
    }

    @Override
    public int bits() {
        return vk;
    }

    @Override
    public int getEnum() {
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

}
