package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.VertexBuffer;
import org.lwjgl.system.MemoryStack;

import java.util.Collection;

public interface VertexPipeline extends Pipeline {

    Integer getAttributeLocation(String attributeName);

    void bindVertexBuffers(MemoryStack stack, CommandBuffer cmd, Collection<VertexBuffer> vertexBuffers);

}
