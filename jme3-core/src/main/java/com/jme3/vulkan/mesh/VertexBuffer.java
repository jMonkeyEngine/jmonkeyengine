package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.nio.*;

public interface VertexBuffer {

    PointerBuffer map();

    void unmap();

    MemorySize size();

    GpuBuffer getBuffer();

    void setNumVertices(int vertices);

    void setAccessFrequency(AccessFrequency access);

    long getOffset();

    boolean isInstanceBuffer();

    default ByteBuffer mapBytes() {
        return map().getByteBuffer(0, size().getBytes());
    }

    default ShortBuffer mapShorts() {
        return map().getShortBuffer(0, size().getShorts());
    }

    default IntBuffer mapInts() {
        return map().getIntBuffer(0, size().getInts());
    }

    default FloatBuffer mapFloats() {
        return map().getFloatBuffer(0, size().getFloats());
    }

    default DoubleBuffer mapDoubles() {
        return map().getDoubleBuffer(0, size().getDoubles());
    }

    default LongBuffer mapLongs() {
        return map().getLongBuffer(0, size().getLongs());
    }

}
