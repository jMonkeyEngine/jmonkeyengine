package com.jme3.vulkan.pipeline.graphics;

import com.jme3.asset.AssetManager;
import com.jme3.util.Version;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.shader.Define;
import com.jme3.vulkan.shader.ShaderDefines;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;

public class NewShaderState {

    public static final String DEFAULT_ENTRY_POINT = "main";

    private final String assetName;
    private final ShaderStage stage;
    private final String entryPoint;
    private final Map<String, String> defines = new HashMap<>();

    public NewShaderState(String assetName, ShaderStage stage, Collection<Define> defines) {
        this(assetName, stage, DEFAULT_ENTRY_POINT, defines);
    }

    public NewShaderState(String assetName, ShaderStage stage, String entryPoint, Collection<Define> defines) {
        this.assetName = assetName;
        this.stage = stage;
        this.entryPoint = entryPoint;
        for (Define d : defines) {
            if (d.isDefineActive()) {
                this.defines.put(d.getDefineName(), d.getDefineValue());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NewShaderState that = (NewShaderState) o;
        return Objects.equals(assetName, that.assetName)
                && Objects.equals(stage, that.stage)
                && Objects.equals(entryPoint, that.entryPoint)
                && Objects.equals(defines, that.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetName, stage, entryPoint, defines);
    }

    public ShaderModule createShader(LogicalDevice<?> device, AssetManager assetManager) {
        return new ShaderModule(device, assetManager, this);
    }

    public String getAssetName() {
        return assetName;
    }

    public ShaderStage getStage() {
        return stage;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public Map<String, String> getDefines() {
        return Collections.unmodifiableMap(defines);
    }

}
