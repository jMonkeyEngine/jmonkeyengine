package com.jme3.vulkan.pipeline.state;

import com.jme3.vulkan.mesh.VertexInput;
import com.jme3.vulkan.pipeline.AbstractVulkanPipeline;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.util.Flag;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanGraphicsPipelineState extends GraphicsState {

    public enum Create implements Flag<AbstractVulkanPipeline.Create> {

        Derivative(VK_PIPELINE_CREATE_DERIVATIVE_BIT),
        AllowDerivatives(VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final Map<String, Integer> attributeLocations = new HashMap<>();

    private PipelineLayout layout;
    private VertexInput vertexInput;
    private Flag<Create> createFlags;
    private int rasterizationSamples = 1;


}
