package com.jme3.vulkan.alloc;

import java.nio.ByteBuffer;

public interface Mappable {

    ByteBuffer map();

    void unmap();

}
