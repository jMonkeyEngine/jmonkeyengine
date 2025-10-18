package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class MultisampleState implements PipelineState<VkPipelineMultisampleStateCreateInfo> {

    private int rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    private boolean sampleShading = false;
    protected long version = 0L;

    @Override
    public VkPipelineMultisampleStateCreateInfo create(MemoryStack stack) {
        return VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineMultisampleStateCreateInfo fill(MemoryStack stack, VkPipelineMultisampleStateCreateInfo struct) {
        return struct.sampleShadingEnable(sampleShading)
                .rasterizationSamples(rasterizationSamples);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MultisampleState that = (MultisampleState) o;
        return rasterizationSamples == that.rasterizationSamples
                && sampleShading == that.sampleShading;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rasterizationSamples, sampleShading);
    }

    public void setRasterizationSamples(int rasterizationSamples) {
        if (this.rasterizationSamples != rasterizationSamples) {
            this.rasterizationSamples = rasterizationSamples;
            version++;
        }
    }

    public void setSampleShading(boolean sampleShading) {
        if (this.sampleShading != sampleShading) {
            this.sampleShading = sampleShading;
            version++;
        }
    }

}
