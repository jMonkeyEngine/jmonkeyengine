package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.pipelines.CullMode;
import com.jme3.vulkan.pipelines.FaceWinding;
import com.jme3.vulkan.pipelines.PolygonMode;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class RasterizationState implements PipelineState<VkPipelineRasterizationStateCreateInfo> {

    private IntEnum<PolygonMode> polygonMode = PolygonMode.Fill;
    private Flag<CullMode> cullMode = CullMode.Back;
    private IntEnum<FaceWinding> faceWinding = FaceWinding.Clockwise;
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
                .polygonMode(polygonMode.getEnum())
                .lineWidth(lineWidth)
                .cullMode(cullMode.bits())
                .frontFace(faceWinding.getEnum())
                .depthBiasEnable(depthBias);
    }

    public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
        this.polygonMode = polygonMode;
    }

    public void setCullMode(Flag<CullMode> cullMode) {
        this.cullMode = cullMode;
    }

    public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
        this.faceWinding = faceWinding;
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
