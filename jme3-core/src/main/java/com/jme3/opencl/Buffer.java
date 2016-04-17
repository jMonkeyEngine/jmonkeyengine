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
public class Buffer {

	private final long buffer;

	public Buffer(long buffer) {
		this.buffer = buffer;
	}
	
	public int getSize() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public MemoryAccess getMemoryAccessFlags() {
		throw new UnsupportedOperationException("not supported yet");
	}
	
	public void read(CommandQueue queue, Buffer src, ByteBuffer dest, int size, int offset) {
		queue.read(src, dest, size, offset);
	}
	public void read(CommandQueue queue, Buffer src, ByteBuffer dest, int size) {
		queue.read(src, dest, size);
	}
	public void read(CommandQueue queue, Buffer src, ByteBuffer dest) {
		queue.read(src, dest);
	}
	
	public Event readAsync(CommandQueue queue, Buffer src, ByteBuffer dest, int size, int offset) {
		return queue.readAsync(src, dest, size, offset);
	}
	public Event readAsync(CommandQueue queue, Buffer src, ByteBuffer dest, int size) {
		return queue.readAsync(src, dest, size);
	}
	public Event readAsync(CommandQueue queue, Buffer src, ByteBuffer dest) {
		return queue.readAsync(src, dest);
	}
	
	public void write(CommandQueue queue, ByteBuffer src, Buffer dest, int size, int offset) {
		queue.write(src, dest, size, offset);
	}
	public void write(CommandQueue queue, ByteBuffer src, Buffer dest, int size) {
		queue.write(src, dest, size);
	}
	public void write(CommandQueue queue, ByteBuffer src, Buffer dest) {
		queue.write(src, dest);
	}
	
	public Event writeAsync(CommandQueue queue, ByteBuffer src, Buffer dest, int size, int offset) {
		return queue.writeAsync(src, dest, size, offset);
	}
	public Event writeAsync(CommandQueue queue, ByteBuffer src, Buffer dest, int size) {
		return queue.writeAsync(src, dest, size);
	}
	public Event writeAsync(CommandQueue queue, ByteBuffer src, Buffer dest) {
		return queue.writeAsync(src, dest);
	}
	
	public void copyTo(CommandQueue queue, Buffer src, Buffer dest, int size, int srcOffset, int destOffset) {
		queue.copyTo(src, dest, size, srcOffset, destOffset);
	}
	public void copyTo(CommandQueue queue, Buffer src, Buffer dest, int size) {
		queue.copyTo(src, dest, size);
	}
	public void copyTo(CommandQueue queue, Buffer src, Buffer dest) {
		queue.copyTo(src, dest);
	}
	
	public Event copyToAsync(CommandQueue queue, Buffer src, Buffer dest, int size, int srcOffset, int destOffset) {
		return queue.copyToAsync(src, dest, size, srcOffset, destOffset);
	}
	public Event copyToAsync(CommandQueue queue, Buffer src, Buffer dest, int size) {
		return queue.copyToAsync(src, dest, size);
	}
	public Event copyToAsync(CommandQueue queue, Buffer src, Buffer dest) {
		return queue.copyToAsync(src, dest);
	}
	
	public ByteBuffer map(CommandQueue queue, Buffer src, int size, int offset, MappingAccess access) {
		return queue.map(src, size, offset, access);
	}
	public ByteBuffer map(CommandQueue queue, Buffer src, int size, MappingAccess access) {
		return queue.map(src, size, access);
	}
	public ByteBuffer map(CommandQueue queue, Buffer src, MappingAccess access) {
		return queue.map(src, access);
	}
	public void unmap(CommandQueue queue, Buffer src, ByteBuffer ptr) {
		queue.unmap(src, ptr);
	}
	
	//TODO: async mapping
}
