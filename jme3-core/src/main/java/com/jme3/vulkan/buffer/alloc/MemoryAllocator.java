package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.images.newimage.ImageInfo;
import com.jme3.vulkan.util.Flag;

public interface MemoryAllocator {

    EngineBuffer createBuffer(BufferType type, int capacity, Flag<EngineBuffer.Role> roles);

    EngineImage createImage(ImageInfo info);

}
