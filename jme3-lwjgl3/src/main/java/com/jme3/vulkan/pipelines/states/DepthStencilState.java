package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;

public class DepthStencilState implements PipelineState<VkPipelineDepthStencilStateCreateInfo> {

    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean depthBoundsTest = false;
    private boolean stencilTest = false;
    private int depthCompare = VK10.VK_COMPARE_OP_LESS_OR_EQUAL;

    @Override
    public VkPipelineDepthStencilStateCreateInfo toStruct(MemoryStack stack) {
        return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(depthTest)
                .depthWriteEnable(depthWrite)
                .depthBoundsTestEnable(depthBoundsTest)
                .stencilTestEnable(stencilTest)
                .depthCompareOp(depthCompare);
    }

    public void setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
    }

    public void setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
    }

    public void setDepthBoundsTest(boolean depthBoundsTest) {
        this.depthBoundsTest = depthBoundsTest;
    }

    public void setStencilTest(boolean stencilTest) {
        this.stencilTest = stencilTest;
    }

    public void setDepthCompare(int depthCompare) {
        this.depthCompare = depthCompare;
    }

}
