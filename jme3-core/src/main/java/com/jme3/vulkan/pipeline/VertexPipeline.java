package com.jme3.vulkan.pipeline;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.VertexAttr;
import com.jme3.vulkan.mesh.VertexBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;

public interface VertexPipeline extends Pipeline {

    Integer getAttributeLocation(String attributeName);

    default void bindVertexBuffers(CommandBuffer cmd, Collection<VertexBuffer> vertexBuffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            Set<Integer> filledAttrLoc = new HashSet<>();
            for (VertexBuffer<Struct<VertexAttr>> vb : vertexBuffers) {
                for (VertexAttr a : vb.getStruct().getFields()) {
                    Integer loc = getAttributeLocation(a.getName());
                    if (loc != null && filledAttrLoc.add(loc)) {
                        VulkanBuffer buffer = (VulkanBuffer) vb.getBuffer();
                        verts.put(buffer.getBufferHandle(cmd.getPool().getDevice()));
                        offsets.put(buffer.size().getOffset());
                        break;
                    }
                }
            }
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts.flip(), offsets.flip());
        }
    }

}
