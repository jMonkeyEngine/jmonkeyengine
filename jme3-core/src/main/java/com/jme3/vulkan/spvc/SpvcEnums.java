package com.jme3.vulkan.spvc;

import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.shaderc.ShaderType;
import org.lwjgl.util.spvc.Spv;

public class SpvcEnums implements EnumInterpreter {

    public static SpvcEnums instance = new SpvcEnums();

    @Override
    public int getShaderTypeEnum(ShaderType type) {
        switch (type) {
            case Vertex: return Spv.SpvExecutionModelVertex;
            case Geometry: return Spv.SpvExecutionModelGeometry;
            case TessellationEval: return Spv.SpvExecutionModelTessellationEvaluation;
            case TessellationControl: return Spv.SpvExecutionModelTessellationControl;
            case Fragment: return Spv.SpvExecutionModelFragment;
            case Compute: return Spv.SpvExecutionModelGLCompute;
            default: throw new UnsupportedOperationException(type.name());
        }
    }

    public ShaderType getShaderType(int type) {
        switch (type) {
            case Spv.SpvExecutionModelVertex: return ShaderType.Vertex;
            case Spv.SpvExecutionModelGeometry: return ShaderType.Geometry;
            case Spv.SpvExecutionModelTessellationEvaluation: return ShaderType.TessellationEval;
            case Spv.SpvExecutionModelTessellationControl: return ShaderType.TessellationControl;
            case Spv.SpvExecutionModelFragment: return ShaderType.Fragment;
            case Spv.SpvExecutionModelGLCompute: return ShaderType.Compute;
            default: throw new UnsupportedOperationException(Integer.toString(type));
        }
    }

}
