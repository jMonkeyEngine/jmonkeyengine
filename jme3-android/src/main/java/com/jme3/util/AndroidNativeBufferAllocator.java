/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Allocates and destroys direct byte buffers using native code.
 *
 * @author pavl_g.
 */
public final class AndroidNativeBufferAllocator implements BufferAllocator {

    static {
        System.loadLibrary("bufferallocatorjme");
    }

    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        releaseDirectByteBuffer(toBeDestroyed);
    }

    @Override
    public ByteBuffer allocate(int size) {
        return createDirectByteBuffer(size);
    }

    /**
     * Releases the memory of a direct buffer using a buffer object reference.
     *
     * @param buffer the buffer reference to release its memory.
     * @see AndroidNativeBufferAllocator#destroyDirectBuffer(Buffer)
     */
    private native void releaseDirectByteBuffer(Buffer buffer);

    /**
     * Creates a new direct byte buffer explicitly with a specific size.
     *
     * @param size the byte buffer size used for allocating the buffer.
     * @return a new direct byte buffer object.
     * @see AndroidNativeBufferAllocator#allocate(int)
     */
    private native ByteBuffer createDirectByteBuffer(long size);
}