package com.jme3.vulkan.shaderc;

import com.jme3.vulkan.formats.EnumInterpreter;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;

public enum ShaderType {

    Vertex,
    Fragment,
    TessellationEval,
    TessellationControl,
    Geometry,
    Compute;

    public int getEnum(EnumInterpreter interpreter) {
        return interpreter.getShaderTypeEnum(this);
    }

}
