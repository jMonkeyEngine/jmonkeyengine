package com.jme3.vulkan.pipeline;

/**
 * A Vulkan pipeline, bound with {@link com.jme3.vulkan.commands.CommandBuffer#cmdBindPipeline}.
 */
public interface Pipeline {

    long getHandle();

    PipelineLayout getLayout();

    PipelineBindPoint getBindPoint();

    /**
     * @param state dynamic state
     * @return true if {@code state} must be set on the command buffer instead of coming from the pipeline
     */
    boolean isDynamic(DynamicState state);

}
