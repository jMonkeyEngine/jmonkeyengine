package com.jme3.vulkan.pipeline.graphics;

import com.jme3.vulkan.pipeline.states.PipelineState;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class ShaderState implements PipelineState<VkPipelineShaderStageCreateInfo> {

    public static final String DEFAULT_ENTRY_POINT = "main";

    private Flag<ShaderStage> stage;
    private ShaderModule module;
    private String entryPoint;
    protected long version = 0L;

    public ShaderState(Flag<ShaderStage> stage, ShaderModule module) {
        this(stage, module, DEFAULT_ENTRY_POINT);
    }

    public ShaderState(Flag<ShaderStage> stage, ShaderModule module, String entryPoint) {
        setStage(stage);
        this.module = module;
        this.entryPoint = entryPoint;
    }

    @Override
    public VkPipelineShaderStageCreateInfo create(MemoryStack stack) {
        return VkPipelineShaderStageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
    }

    @Override
    public VkPipelineShaderStageCreateInfo fill(MemoryStack stack, VkPipelineShaderStageCreateInfo struct) {
        return struct.stage(stage.lowestBit())
                .module(module.getNativeObject())
                .pName(stack.UTF8(entryPoint));
    }

    @Override
    public ShaderState copy() {
        return new ShaderState(stage, module, entryPoint);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderState that = (ShaderState) o;
        return Flag.is(stage, that.stage)
                && module == that.module
                && Objects.equals(entryPoint, that.entryPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage, module, entryPoint);
    }

    public void setStage(Flag<ShaderStage> stage) {
        if (!Flag.is(this.stage, stage)) {
            if (stage.bitCount() > 1) {
                throw new IllegalArgumentException("Flag must specify only one shader stage.");
            }
            this.stage = stage;
            version++;
        }
    }

    public void setModule(ShaderModule module) {
        if (this.module != module) {
            this.module = module;
            version++;
        }
    }

    public void setEntryPoint(String entryPoint) {
        if (!Objects.equals(this.entryPoint, entryPoint)) {
            this.entryPoint = entryPoint;
            version++;
        }
    }

    public Flag<ShaderStage> getStage() {
        return stage;
    }

    public ShaderModule getModule() {
        return module;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

}
