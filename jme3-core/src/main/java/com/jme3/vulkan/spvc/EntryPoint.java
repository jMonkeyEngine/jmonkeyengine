package com.jme3.vulkan.spvc;

import com.jme3.vulkan.shaderc.ShaderType;
import org.lwjgl.util.spvc.SpvcEntryPoint;

public class EntryPoint {

    private final String name;
    private final ShaderType type;

    public EntryPoint(String name, ShaderType type) {
        this.name = name;
        this.type = type;
    }

    public EntryPoint(SpvcEntryPoint p) {
        this.name = p.nameString();
        this.type = SpvcEnums.instance.getShaderType(p.execution_model());
    }

    public String getName() {
        return name;
    }

    public ShaderType getType() {
        return type;
    }

}
