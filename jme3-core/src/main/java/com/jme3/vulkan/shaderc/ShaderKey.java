package com.jme3.vulkan.shaderc;

import com.jme3.asset.AssetKey;
import com.jme3.vulkan.material.shader.ShaderStage;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ShaderKey extends AssetKey<ByteBuffer> {

    private final ShaderStage stage;
    private final Map<String, String> defines = new HashMap<>();

    public ShaderKey(ShaderStage stage, String name) {
        super(name);
        this.stage = stage;
    }

    public void setDefine(String name, String value) {
        defines.put(name, value);
    }

    public void setDefines(Map<String, String> defines) {
        this.defines.putAll(defines);
    }

    public ShaderStage getStage() {
        return stage;
    }

    public Map<String, String> getDefines() {
        return Collections.unmodifiableMap(defines);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ShaderKey shaderKey = (ShaderKey)o;
        return stage == shaderKey.stage && Objects.equals(defines, shaderKey.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stage, defines);
    }

}
