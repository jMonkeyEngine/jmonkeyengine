package com.jme3.vulkan.pipeline.states;

import com.jme3.vulkan.shader.ShaderStage;

import java.util.Arrays;
import java.util.Objects;

public class IShaderState {

    private final String assetName;
    private final String entryPoint;
    private final ShaderStage stage;

    public IShaderState(String assetName, String entryPoint, ShaderStage stage) {
        this.assetName = assetName;
        this.entryPoint = entryPoint;
        this.stage = stage;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public ShaderStage getStage() {
        return stage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IShaderState that = (IShaderState) o;
        return Objects.equals(assetName, that.assetName)
                && Objects.equals(entryPoint, that.entryPoint)
                && Objects.equals(stage, that.stage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetName, entryPoint, stage);
    }

}
