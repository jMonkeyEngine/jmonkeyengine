package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Mesh {

    private GpuBuffer indexBuffer;
    private final List<GpuBuffer> vertexBuffers = new ArrayList<>();

    public Mesh() {

    }

    public void bindVertexBuffers(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer vertBufs = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (GpuBuffer v : vertexBuffers) {
                vertBufs.put(v.getNativeObject());
                offsets.put(0);
            }
            vertBufs.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, vertBufs, offsets);
            vkCmdBindIndexBuffer(cmd.getBuffer(), indexBuffer.getNativeObject(), 0, VK_INDEX_TYPE_UINT32);
        }
    }

    public void draw(CommandBuffer cmd) {
        vkCmdDrawIndexed(cmd.getBuffer(), indexBuffer.size().getElements(), 1, 0, 0, 0);
    }

}
