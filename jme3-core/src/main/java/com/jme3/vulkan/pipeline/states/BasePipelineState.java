package com.jme3.vulkan.pipeline.states;

import com.jme3.util.Versionable;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.material.shader.ShaderModule;
import org.lwjgl.system.MemoryStack;

import java.util.Collection;

public interface BasePipelineState <SELF extends BasePipelineState, T> extends Versionable {

    /**
     * Creates a struct that can hold this state. Only the {@code sType}
     * field should be set by this method.
     *
     * @param stack memory stack
     * @return struct that may contain this state
     */
    T create(MemoryStack stack);

    /**
     * Fill {@code struct} with this state.
     *
     * @param stack memory stack
     * @param struct struct to fill
     * @param shaders {@link #getPipelineShaderStates() shaders} associated with this state
     * @return filled struct
     */
    T fill(MemoryStack stack, T struct, Collection<ShaderModule> shaders);

    /**
     * Selects a pipeline to use based on the current state.
     *
     * @param cache cache that may be used for selection
     * @param mesh mesh description to select with
     * @return selected pipeline
     */
    Pipeline selectPipeline(PipelineCache cache, MeshLayout mesh);

    /**
     * Creates a new pipeline based on the current state.
     *
     * @param device logical device
     * @param parent the new pipeline's parent (can be null)
     * @param shaders shaders created from {@link #getPipelineShaderStates()} to use in pipeline creation
     * @return new pipeline
     */
    Pipeline createPipeline(LogicalDevice<?> device, Pipeline parent, Collection<ShaderModule> shaders);

    /**
     *
     * @return shader states
     */
    Collection<IShaderState> getPipelineShaderStates();

    /**
     * Create a copy of this state.
     *
     * @return copy stores the copy result (or null to create a new state)
     */
    SELF copy(SELF store);

    /**
     * Overrides the parameters of {@code state} according to which
     * parameters of this state have been set.
     *
     * @param state state to override
     * @param store state to store the result (or null to create a new state)
     * @return result of override operation
     */
    SELF override(SELF state, SELF store);

    /**
     * Returns the base class representing {@link SELF}.
     *
     * @return class of {@link SELF}
     */
    Class<SELF> getBaseStateClass();

    /**
     * {@link #copy(BasePipelineState) Copies} this state to a new state.
     *
     * @return state copying this state
     */
    default SELF copy() {
        return copy(null);
    }

    /**
     * {@link #override(BasePipelineState, BasePipelineState) Overrides} {@code state}
     * with this state's parameters.
     *
     * @param state state to override
     * @return result of override operation
     */
    default SELF override(SELF state) {
        return override(state, null);
    }

    /**
     * {@link #copy(BasePipelineState) Copies} this state to {@code store} if {@code store} is an instance
     * of {@link #getBaseStateClass()}. Otherwise a new state is created
     * to hold the copy.
     *
     * @param store stores the copy of this state, if possible (or null to create a new state)
     * @return copy of this state
     */
    @SuppressWarnings("unchecked")
    default SELF copyAnonymous(BasePipelineState<?, ?> store) {
        if (store == null || getBaseStateClass().isAssignableFrom(store.getClass())) {
            return copy((SELF)store);
        } else {
            return copy(null);
        }
    }

    /**
     * {@link #override(BasePipelineState, BasePipelineState) Overrides} {@code state} with this
     * state's parameters. If {@code state} is not an instance of {@link #getBaseStateClass()},
     * then this state's parameters are {@link #copyAnonymous(BasePipelineState) copied anonymously}
     * to {@code store}.
     *
     * @param state state to override (not null)
     * @param store state to store the override operation (or null to create a new state)
     * @return result of the override operation
     */
    @SuppressWarnings("unchecked")
    default SELF overrideAnonymous(BasePipelineState<?, ?> state, BasePipelineState<?, ?> store) {
        if (getBaseStateClass().isAssignableFrom(state.getClass())
                && (store == null || getBaseStateClass().isAssignableFrom(store.getClass()))) {
            return override((SELF)state, (SELF)store);
        } else {
            return copyAnonymous(store);
        }
    }

}
