package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.material.shader.ShaderStage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

/**
 * One shader stage of a pipeline: a shader module and the entry point to run in it. The module
 * only needs to exist while pipelines are created from it.
 */
public final class PipelineShader {

    private final ShaderStage stage;
    private final long module;
    private final String entryPoint;

    /**
     * @param stage single stage the entry point runs in
     * @param module shader module handle
     * @param entryPoint name of the entry point function
     */
    public PipelineShader(ShaderStage stage, long module, String entryPoint) {
        if (Integer.bitCount(stage.bits()) != 1) {
            throw new IllegalArgumentException("Pipeline shader must be for exactly one stage: " + stage);
        }
        this.stage = stage;
        this.module = module;
        this.entryPoint = entryPoint;
    }

    public void fill(MemoryStack stack, VkPipelineShaderStageCreateInfo info) {
        info.sType$Default()
                .stage(stage.bits())
                .module(module)
                .pName(stack.UTF8(entryPoint));
    }

    public ShaderStage getStage() {
        return stage;
    }

    public long getModule() {
        return module;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

}
