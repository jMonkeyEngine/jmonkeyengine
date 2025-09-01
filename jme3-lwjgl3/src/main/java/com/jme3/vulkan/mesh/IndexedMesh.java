package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public abstract class IndexedMesh implements Mesh {

    protected final List<GpuBuffer> vertexBuffers = new ArrayList<>();
    protected GpuBuffer indexBuffer;

    @Override
    public void bind(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer vertBufs = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (GpuBuffer vb : vertexBuffers) {
                vertBufs.put(vb.getId());
                offsets.put(0);
            }
            vertBufs.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, vertBufs, offsets);
        }
        vkCmdBindIndexBuffer(cmd.getBuffer(), indexBuffer.getId(), 0, IndexType.UInt32.getEnum());
    }

    @Override
    public void draw(CommandBuffer cmd) {
        vkCmdDrawIndexed(cmd.getBuffer(), indexBuffer.size().getElements(), 1, 0, 0, 0);
    }

}
