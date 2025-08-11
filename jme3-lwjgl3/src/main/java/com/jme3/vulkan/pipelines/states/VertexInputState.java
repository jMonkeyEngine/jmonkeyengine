package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.mesh.MeshDescription;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import java.util.Objects;

public class VertexInputState implements PipelineState<VkPipelineVertexInputStateCreateInfo> {

    private MeshDescription mesh;

    public VertexInputState(MeshDescription mesh) {
        setMesh(mesh);
    }

    @Override
    public VkPipelineVertexInputStateCreateInfo toStruct(MemoryStack stack) {
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(mesh.getBindings(stack))
                .pVertexAttributeDescriptions(mesh.getAttributes(stack));
    }

    public void setMesh(MeshDescription mesh) {
        this.mesh = Objects.requireNonNull(mesh, "Mesh description cannot be null.");
    }

}
