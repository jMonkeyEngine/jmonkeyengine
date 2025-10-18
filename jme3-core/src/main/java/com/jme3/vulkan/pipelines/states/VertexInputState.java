package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.mesh.MeshDescription;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class VertexInputState implements PipelineState<VkPipelineVertexInputStateCreateInfo> {

    private MeshDescription mesh;
    protected long version = 0L;

    public VertexInputState() {}

    public VertexInputState(MeshDescription mesh) {
        this.mesh = mesh;
    }

    @Override
    public VkPipelineVertexInputStateCreateInfo create(MemoryStack stack) {
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineVertexInputStateCreateInfo fill(MemoryStack stack, VkPipelineVertexInputStateCreateInfo struct) {
        return struct.pVertexBindingDescriptions(mesh.getBindingInfo(stack))
                .pVertexAttributeDescriptions(mesh.getAttributeInfo(stack));
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VertexInputState that = (VertexInputState) o;
        return Objects.equals(mesh, that.mesh);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mesh);
    }

    public void setMesh(MeshDescription mesh) {
        if (!Objects.equals(this.mesh, mesh)) {
            this.mesh = mesh;
            version++;
        }
    }

}
