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

    }

    public void draw(CommandBuffer cmd) {
        vkCmdDrawIndexed(cmd.getBuffer(), indexBuffer.size().getElements(), 1, 0, 0, 0);
    }

}
