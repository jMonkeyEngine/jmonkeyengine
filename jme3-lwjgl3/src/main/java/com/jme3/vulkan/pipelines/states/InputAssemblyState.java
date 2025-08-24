package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

public class InputAssemblyState implements PipelineState<VkPipelineInputAssemblyStateCreateInfo> {

    private int topology = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    private boolean primitiveRestart = false;

    @Override
    public VkPipelineInputAssemblyStateCreateInfo toStruct(MemoryStack stack) {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(topology)
                .primitiveRestartEnable(primitiveRestart);
    }

    public void setTopology(int topology) {
        this.topology = topology;
    }

    public void setPrimitiveRestart(boolean primitiveRestart) {
        this.primitiveRestart = primitiveRestart;
    }

}
