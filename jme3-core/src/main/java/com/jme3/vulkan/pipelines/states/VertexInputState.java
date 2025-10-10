package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.mesh.MeshDescription;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VertexInputState implements PipelineState<VkPipelineVertexInputStateCreateInfo> {

    private MeshDescription mesh;

    public VertexInputState() {}

    public VertexInputState(MeshDescription mesh) {
        this.mesh = mesh;
    }

    @Override
    public VkPipelineVertexInputStateCreateInfo toStruct(MemoryStack stack) {
        Objects.requireNonNull(mesh, "Mesh description is not defined.");
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(mesh.getBindingInfo(stack))
                .pVertexAttributeDescriptions(mesh.getAttributeInfo(stack));
    }

    public void setMesh(MeshDescription mesh) {
        this.mesh = mesh;
    }

}
