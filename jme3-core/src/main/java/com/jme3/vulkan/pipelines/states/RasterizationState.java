package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.pipelines.CullMode;
import com.jme3.vulkan.pipelines.FaceWinding;
import com.jme3.vulkan.pipelines.PolygonMode;
import com.jme3.vulkan.pipelines.newstate.PipelineEnum;
import com.jme3.vulkan.pipelines.newstate.PipelineStruct;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;

import java.awt.image.Raster;
import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class RasterizationState implements PipelineState<VkPipelineRasterizationStateCreateInfo> {

    private IntEnum<PolygonMode> polygonMode = PolygonMode.Fill;
    private Flag<CullMode> cullMode = CullMode.Back;
    private IntEnum<FaceWinding> faceWinding = FaceWinding.Clockwise;
    private float lineWidth = 1f;
    private boolean depthClamp = false;
    private boolean rasterizerDiscard = false;
    private boolean depthBias = false;
    protected long version = 0L;

    @Override
    public VkPipelineRasterizationStateCreateInfo create(MemoryStack stack) {
        return VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineRasterizationStateCreateInfo fill(MemoryStack stack, VkPipelineRasterizationStateCreateInfo struct) {
        return struct.depthClampEnable(depthClamp)
                .rasterizerDiscardEnable(rasterizerDiscard)
                .polygonMode(polygonMode.getEnum())
                .lineWidth(lineWidth)
                .cullMode(cullMode.bits())
                .frontFace(faceWinding.getEnum())
                .depthBiasEnable(depthBias);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RasterizationState that = (RasterizationState) o;
        return Float.compare(lineWidth, that.lineWidth) == 0
                && depthClamp == that.depthClamp
                && rasterizerDiscard == that.rasterizerDiscard
                && depthBias == that.depthBias
                && IntEnum.is(polygonMode, that.polygonMode)
                && Flag.is(cullMode, that.cullMode)
                && IntEnum.is(faceWinding, that.faceWinding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(polygonMode, cullMode, faceWinding, lineWidth, depthClamp, rasterizerDiscard, depthBias);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
        if (!IntEnum.is(this.polygonMode, polygonMode)) {
            this.polygonMode = polygonMode;
            version++;
        }
    }

    public void setCullMode(Flag<CullMode> cullMode) {
        if (!Flag.is(this.cullMode, cullMode)) {
            this.cullMode = cullMode;
            version++;
        }
    }

    public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
        if (!IntEnum.is(this.faceWinding, faceWinding)) {
            this.faceWinding = faceWinding;
            version++;
        }
    }

    public void setLineWidth(float lineWidth) {
        if (this.lineWidth != lineWidth) {
            this.lineWidth = lineWidth;
            version++;
        }
    }

    public void setDepthClamp(boolean depthClamp) {
        if (this.depthClamp != depthClamp) {
            this.depthClamp = depthClamp;
            version++;
        }
    }

    public void setRasterizerDiscard(boolean rasterizerDiscard) {
        if (this.rasterizerDiscard != rasterizerDiscard) {
            this.rasterizerDiscard = rasterizerDiscard;
            version++;
        }
    }

    public void setDepthBias(boolean depthBias) {
        if (this.depthBias != depthBias) {
            this.depthBias = depthBias;
            version++;
        }
    }

}
