package com.jme3.vulkan.pipeline.states;

import com.jme3.util.Versionable;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.shader.ShaderModule;
import org.lwjgl.system.MemoryStack;

import java.util.Collection;
import java.util.Map;

public interface BasePipelineState <P extends Pipeline, T> extends Versionable {

    T create(MemoryStack stack);

    T fill(MemoryStack stack, T struct, Collection<ShaderModule> shaders);

    /**
     * Selects a pipeline to use based on the current state.
     *
     * @param cache cache that may be used for selection
     * @return selected pipeline
     */
    P selectPipeline(PipelineCache cache, MeshDescription mesh);

    /**
     * Creates a new pipeline based on the current state.
     *
     * @param device logical device
     * @param parent the new pipeline's parent (can be null)
     * @return new pipeline
     */
    P createPipeline(LogicalDevice<?> device, Pipeline parent, Collection<ShaderModule> shaders);

    /**
     *
     * @return shader states
     */
    Collection<IShaderState> getPipelineShaderStates();

    /**
     * Create a copy of this state.
     *
     * @return copy
     */
    BasePipelineState<P, T> copy();

}
