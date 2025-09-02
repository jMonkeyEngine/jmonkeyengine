package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.data.DataPipe;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements Mesh {

    protected final MeshDescription description;
    protected final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    protected VertexBuffer indexBuffer;

    public AdaptiveMesh(MeshDescription description) {
        this.description = description;
    }

    @Override
    public void update(CommandBuffer cmd) {
        for (VertexBuffer vb : vertexBuffers) {
            vb.update(cmd);
        }
        indexBuffer.update(cmd);
    }

    @Override
    public void bind(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (VertexBuffer vb : vertexBuffers) {
                verts.put(vb.buffer.getId());
                offsets.put(0L);
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        vkCmdBindIndexBuffer(cmd.getBuffer(), indexBuffer.buffer.getId(), 0, IndexType.UInt32.getEnum());
    }

    @Override
    public void draw(CommandBuffer cmd) {
        vkCmdDrawIndexed(cmd.getBuffer(), indexBuffer.buffer.size().getElements(), 1, 0, 0, 0);
    }

    protected static class VertexBuffer {

        private DataPipe<? extends GpuBuffer> pipe;
        private GpuBuffer buffer;

        public void update(CommandBuffer cmd) {
            buffer = pipe.execute(cmd);
        }

    }

}
