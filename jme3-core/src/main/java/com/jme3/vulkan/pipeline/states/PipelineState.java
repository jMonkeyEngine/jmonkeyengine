package com.jme3.vulkan.pipeline.states;

import org.lwjgl.system.MemoryStack;

public interface PipelineState <T> {

    /**
     * Creates the struct but does not fill the struct with
     * this state's information. Only the {@code sType} field
     * should be written by this method.
     *
     * @param stack memory stack
     * @return struct
     */
    T create(MemoryStack stack);

    /**
     * Fills {@code struct} with this state's information.
     *
     * @param stack memory stack
     * @param struct struct to fill
     * @return filled struct
     */
    T fill(MemoryStack stack, T struct);

    /**
     * Creates a copy of this state.
     *
     * @return copy
     */
    PipelineState<T> copy();

    /**
     * Returns the version number representing the current state.
     * If any state is changed, the version number returned by
     * this method increases.
     *
     * @return version number for the current state (not negative)
     */
    long getCurrentVersion();

    /**
     * Creates and fills a new struct with this state's information.
     *
     * @param stack memory stack
     * @return created and filled struct
     */
    default T fill(MemoryStack stack) {
        return fill(stack, create(stack));
    }

    /**
     * Gets the {@link #getCurrentVersion() version} of {@code state}.
     * If {@code state} is null {@code 0L} is returned.
     *
     * @param state state to get version of
     * @return version of the state, or 0 if state is null
     */
    static long versionOf(PipelineState<?> state) {
        return state != null ? state.getCurrentVersion() : 0L;
    }

}
