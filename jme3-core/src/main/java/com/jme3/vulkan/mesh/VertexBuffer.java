package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.frames.VersionedResource;
import org.lwjgl.PointerBuffer;

import java.nio.*;

public class VertexBuffer {

    private final VersionedResource<? extends GpuBuffer> resource;
    private GpuBuffer buffer;
    private int mappers = 0;
    private PointerBuffer memory;

    public VertexBuffer(VersionedResource<? extends GpuBuffer> resource) {
        this.resource = resource;
    }

    public PointerBuffer map() {
        if (mappers++ == 0) {
            memory = (buffer = resource.get()).map();
        }
        // the mapped buffer could only ever not match the frame's current buffer if
        // not all mappings were cleared on the previous frame (i.e. mappers != 0)
        if (buffer != resource.get()) {
            throw new IllegalStateException("Not all mappings were properly unmapped.");
        }
        return memory;
    }

    public void unmap() {
        if (--mappers <= 0) {
            memory = null;
            buffer.unmap();
        }
    }

    public ByteBuffer mapBytes() {
        return map().getByteBuffer(0, buffer.size().getBytes());
    }

    public ShortBuffer mapShorts() {
        return map().getShortBuffer(0, buffer.size().getShorts());
    }

    public IntBuffer mapInts() {
        return map().getIntBuffer(0, buffer.size().getInts());
    }

    public FloatBuffer mapFloats() {
        return map().getFloatBuffer(0, buffer.size().getFloats());
    }

    public DoubleBuffer mapDoubles() {
        return map().getDoubleBuffer(0, buffer.size().getDoubles());
    }

    public LongBuffer mapLongs() {
        return map().getLongBuffer(0, buffer.size().getLongs());
    }

    public VersionedResource<? extends GpuBuffer> getResource() {
        return resource;
    }

}
