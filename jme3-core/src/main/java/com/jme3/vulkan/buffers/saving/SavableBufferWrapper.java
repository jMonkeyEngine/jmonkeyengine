package com.jme3.vulkan.buffers.saving;

import com.jme3.export.*;
import com.jme3.vulkan.buffers.MappableBuffer;

public interface SavableBufferWrapper <T extends MappableBuffer> extends Savable {

    T getBuffer();

}
