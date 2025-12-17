package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.nio.*;

public interface Mappable {

    PointerBuffer map();

    void unmap();

    MemorySize size();

    ByteBuffer mapBytes();

    ShortBuffer mapShorts();

    IntBuffer mapInts();

    FloatBuffer mapFloats();

    DoubleBuffer mapDoubles();

    LongBuffer mapLongs();

}
