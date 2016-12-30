package com.jme3.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * The implementation of the {@link BufferAllocator} which use {@link MemoryUtil} to manage memory.
 *
 * @author JavaSaBr
 */
public class LWJGLBufferAllocator implements BufferAllocator {

    @Override
    public void destroyDirectBuffer(final Buffer buffer) {
        MemoryUtil.memFree(buffer);
    }

    @Override
    public ByteBuffer allocate(final int size) {
        return MemoryUtil.memAlloc(size);
    }
}
