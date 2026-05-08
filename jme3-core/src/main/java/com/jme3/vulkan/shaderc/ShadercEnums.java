package com.jme3.vulkan.shaderc;

import com.jme3.vulkan.formats.EnumInterpreter;
import org.lwjgl.util.shaderc.Shaderc;

public class ShadercEnums implements EnumInterpreter {

    public static ShadercEnums instance = new ShadercEnums();

    @Override
    public int getShaderTypeEnum(ShaderType type) {
        switch (type) {
            case Vertex: return Shaderc.shaderc_vertex_shader;
            case Geometry: return Shaderc.shaderc_geometry_shader;
            case TessellationEval: return Shaderc.shaderc_tess_evaluation_shader;
            case TessellationControl: return Shaderc.shaderc_tess_control_shader;
            case Fragment: return Shaderc.shaderc_fragment_shader;
            case Compute: return Shaderc.shaderc_compute_shader;
            default: throw new UnsupportedOperationException(type.name());
        }
    }

}
