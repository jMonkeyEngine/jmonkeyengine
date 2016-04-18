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
public final class CommandQueue extends NativeCLObject {

    private final long queue;

    public CommandQueue(long queue) {
        this.queue = queue;
    }

    public void read(Buffer src, ByteBuffer dest, int size, int offset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void read(Buffer src, ByteBuffer dest, int size) {
        read(src, dest, size, 0);
    }

    public void read(Buffer src, ByteBuffer dest) {
        read(src, dest, src.getSize(), 0);
    }

    public Event readAsync(Buffer src, ByteBuffer dest, int size, int offset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Event readAsync(Buffer src, ByteBuffer dest, int size) {
        return readAsync(src, dest, size, 0);
    }

    public Event readAsync(Buffer src, ByteBuffer dest) {
        return readAsync(src, dest, src.getSize());
    }

    public void write(ByteBuffer src, Buffer dest, int size, int offset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void write(ByteBuffer src, Buffer dest, int size) {
        write(src, dest, size, 0);
    }

    public void write(ByteBuffer src, Buffer dest) {
        write(src, dest, dest.getSize());
    }

    public Event writeAsync(ByteBuffer src, Buffer dest, int size, int offset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Event writeAsync(ByteBuffer src, Buffer dest, int size) {
        return writeAsync(src, dest, size, 0);
    }

    public Event writeAsync(ByteBuffer src, Buffer dest) {
        return writeAsync(src, dest, dest.getSize());
    }

    public void copyTo(Buffer src, Buffer dest, int size, int srcOffset, int destOffset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void copyTo(Buffer src, Buffer dest, int size) {
        copyTo(src, dest, size, 0, 0);
    }

    public void copyTo(Buffer src, Buffer dest) {
        copyTo(src, dest, src.getSize());
    }

    public Event copyToAsync(Buffer src, Buffer dest, int size, int srcOffset, int destOffset) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Event copyToAsync(Buffer src, Buffer dest, int size) {
        return copyToAsync(src, dest, size, 0, 0);
    }

    public Event copyToAsync(Buffer src, Buffer dest) {
        return copyToAsync(src, dest, src.getSize());
    }

    public ByteBuffer map(Buffer src, int size, int offset, MappingAccess access) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public ByteBuffer map(Buffer src, int size, MappingAccess access) {
        return map(src, size, 0, access);
    }

    public ByteBuffer map(Buffer src, MappingAccess access) {
        return map(src, src.getSize(), access);
    }

    public void unmap(Buffer src, ByteBuffer ptr) {
        throw new UnsupportedOperationException("not supported yet");
    }

	//TODO: async mapping
	//TODO: clEnqueueFillBuffer
	//TODO: image read/write
    public void flush() {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void finish() {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void deleteObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
