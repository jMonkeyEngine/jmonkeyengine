/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.ngengine.libjglios.core.LibJGLIOSBufferAllocator;


public final class LibJGLIOSNativeBufferAllocator implements BufferAllocator {

    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        LibJGLIOSBufferAllocator.free(toBeDestroyed);
    }

    @Override
    public ByteBuffer allocate(int size) {
        ByteBuffer buffer = LibJGLIOSBufferAllocator.allocate(size);
        if (buffer == null) {
            throw new OutOfMemoryError("Could not allocate " + size + " bytes through libJGLIOS");
        }
        return buffer;
    }
}
