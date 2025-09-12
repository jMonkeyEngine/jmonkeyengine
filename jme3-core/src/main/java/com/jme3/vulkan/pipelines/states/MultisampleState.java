package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class MultisampleState implements PipelineState<VkPipelineMultisampleStateCreateInfo> {

    private int rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    private boolean sampleShading = false;

    @Override
    public VkPipelineMultisampleStateCreateInfo toStruct(MemoryStack stack) {
        return VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(sampleShading)
                .rasterizationSamples(rasterizationSamples);
    }

    public void setRasterizationSamples(int rasterizationSamples) {
        this.rasterizationSamples = rasterizationSamples;
    }

    public void setSampleShading(boolean sampleShading) {
        this.sampleShading = sampleShading;
    }

}
