/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Sebastian Weiss
 */
public abstract class Buffer {

    public abstract int getSize();

    public abstract MemoryAccess getMemoryAccessFlags();

    public abstract void read(CommandQueue queue, ByteBuffer dest, int size, int offset);

    public void read(CommandQueue queue, ByteBuffer dest, int size) {
        read(queue, dest, size, 0);
    }

    public void read(CommandQueue queue, ByteBuffer dest) {
        read(queue, dest, getSize());
    }

    public abstract Event readAsync(CommandQueue queue, ByteBuffer dest, int size, int offset);

    public Event readAsync(CommandQueue queue, ByteBuffer dest, int size) {
        return readAsync(queue, dest, size, 0);
    }

    public Event readAsync(CommandQueue queue, ByteBuffer dest) {
        return readAsync(queue, dest, getSize());
    }

    public abstract void write(CommandQueue queue, ByteBuffer src, int size, int offset);

    public void write(CommandQueue queue, ByteBuffer src, int size) {
        write(queue, src, size, 0);
    }

    public void write(CommandQueue queue, ByteBuffer src) {
        write(queue, src, getSize());
    }

    public abstract Event writeAsync(CommandQueue queue, ByteBuffer src, int size, int offset);

    public Event writeAsync(CommandQueue queue, ByteBuffer src, int size) {
        return writeAsync(queue, src, size, 0);
    }

    public Event writeAsync(CommandQueue queue, ByteBuffer src) {
        return writeAsync(queue, src, getSize());
    }

    public abstract void copyTo(CommandQueue queue, Buffer dest, int size, int srcOffset, int destOffset);

    public void copyTo(CommandQueue queue, Buffer dest, int size) {
        copyTo(queue, dest, size, 0, 0);
    }

    public void copyTo(CommandQueue queue, Buffer dest) {
        copyTo(queue, dest, getSize());
    }

    public abstract Event copyToAsync(CommandQueue queue, Buffer dest, int size, int srcOffset, int destOffset);

    public Event copyToAsync(CommandQueue queue, Buffer dest, int size) {
        return copyToAsync(queue, dest, size, 0, 0);
    }

    public Event copyToAsync(CommandQueue queue, Buffer dest) {
        return copyToAsync(queue, dest, getSize());
    }

    public abstract ByteBuffer map(CommandQueue queue, int size, int offset, MappingAccess access);

    public ByteBuffer map(CommandQueue queue, int size, MappingAccess access) {
        return map(queue, size, 0, access);
    }

    public ByteBuffer map(CommandQueue queue, MappingAccess access) {
        return map(queue, getSize(), access);
    }

    public abstract void unmap(CommandQueue queue, ByteBuffer ptr);

    public abstract AsyncMapping mapAsync(CommandQueue queue, int size, int offset, MappingAccess access);
    public AsyncMapping mapAsync(CommandQueue queue, int size, MappingAccess access) {
        return mapAsync(queue, size, 0, access);
    }
    public AsyncMapping mapAsync(CommandQueue queue, MappingAccess access) {
        return mapAsync(queue, getSize(), 0, access);
    }
    
    public abstract Event fillAsync(CommandQueue queue, ByteBuffer pattern, int size, int offset);

	//TODO: copy to image

    /**
     * Result of an async mapping operation, contains the event and the target byte buffer.
     * This is a work-around since no generic pair-structure is avaiable.
     *
     * @author Sebastian Weiss
     */
    public static class AsyncMapping {

        public final Event event;
        public final ByteBuffer buffer;

        public AsyncMapping(Event event, ByteBuffer buffer) {
            super();
            this.event = event;
            this.buffer = buffer;
        }

        public Event getEvent() {
            return event;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }
    }
    
    public abstract Event copyToImageAsync(CommandQueue queue, Image dest, long srcOffset, long[] destOrigin, long[] destRegion);
}
