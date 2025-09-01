package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class RasterizationState implements PipelineState<VkPipelineRasterizationStateCreateInfo> {

    private int polygonMode = VK_POLYGON_MODE_FILL;
    private int cullMode = VK_CULL_MODE_BACK_BIT;
    private int frontFace = VK_FRONT_FACE_CLOCKWISE;
    private float lineWidth = 1f;
    private boolean depthClamp = false;
    private boolean rasterizerDiscard = false;
    private boolean depthBias = false;

    @Override
    public VkPipelineRasterizationStateCreateInfo toStruct(MemoryStack stack) {
        return VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(depthClamp)
                .rasterizerDiscardEnable(rasterizerDiscard)
                .polygonMode(polygonMode)
                .lineWidth(lineWidth)
                .cullMode(cullMode)
                .frontFace(frontFace)
                .cullMode(cullMode)
                .depthBiasEnable(depthBias);
    }

    public void setPolygonMode(int polygonMode) {
        this.polygonMode = polygonMode;
    }

    public void setCullMode(int cullMode) {
        this.cullMode = cullMode;
    }

    public void setFrontFace(int frontFace) {
        this.frontFace = frontFace;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public void setDepthClamp(boolean depthClamp) {
        this.depthClamp = depthClamp;
    }

    public void setRasterizerDiscard(boolean rasterizerDiscard) {
        this.rasterizerDiscard = rasterizerDiscard;
    }

    public void setDepthBias(boolean depthBias) {
        this.depthBias = depthBias;
    }

}
