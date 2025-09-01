package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;

import java.nio.ByteBuffer;

public class VertexBuffer {

    private final GpuBuffer buffer;
    private ByteBuffer mappedBuffer;
    private int mappers = 0;

    public ByteBuffer map() {
        if (mappers++ == 0) {
            mappedBuffer = buffer.mapBytes();
        }
        return mappedBuffer;
    }

    public void unmap() {
        if ((mappers = Math.max(mappers - 1, 0)) == 0) {
            mappedBuffer = null;
            buffer.unmap();
        }
    }

}
