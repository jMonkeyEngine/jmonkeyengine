package com.jme3.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public interface BufferAllocator {

    void destroyDirectBuffer(Buffer toBeDestroyed);

    ByteBuffer allocate(int size);

}
