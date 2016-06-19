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
 * Wrapper for an OpenCL buffer object.
 * A buffer object stores a one-dimensional collection of elements. Elements of a buffer object can
 * be a scalar data type (such as an int, float), vector data type, or a user-defined structure.
 * <br>
 * Buffers are created by the {@link Context}.
 * <br>
 * All access methods (read/write/copy/map) are available in both sychronized/blocking versions
 * and in async/non-blocking versions. The later ones always return an {@link Event} object
 * and have the prefix -Async in their name.
 * 
 * @see Context#createBuffer(long, com.jme3.opencl.MemoryAccess) 
 * @author shaman
 */
public abstract class Buffer extends AbstractOpenCLObject {

    protected Buffer(ObjectReleaser releaser) {
        super(releaser);
    }

	@Override
	public Buffer register() {
		super.register();
		return this;
	}
    
    /**
     * @return the size of the buffer in bytes.
     * @see Context#createBuffer(long) 
     */
    public abstract long getSize();

    /**
     * @return the memory access flags set on creation.
     * @see Context#createBuffer(long, com.jme3.opencl.MemoryAccess) 
     */
    public abstract MemoryAccess getMemoryAccessFlags();

    /**
     * Performs a blocking read of the buffer.
     * The target buffer must have at least {@code size} bytes remaining.
     * This method may set the limit to the last byte read.
     * @param queue the command queue
     * @param dest the target buffer
     * @param size the size in bytes being read
     * @param offset the offset in bytes in the buffer to read from
     */
    public abstract void read(CommandQueue queue, ByteBuffer dest, long size, long offset);

    /**
     * Alternative version of {@link #read(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long, long) },
     * sets {@code offset} to zero.
     */
    public void read(CommandQueue queue, ByteBuffer dest, long size) {
        read(queue, dest, size, 0);
    }

    /**
     * Alternative version of {@link #read(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long) },
     * sets {@code size} to {@link #getSize() }.
     */
    public void read(CommandQueue queue, ByteBuffer dest) {
        read(queue, dest, getSize());
    }

    /**
     * Performs an async/non-blocking read of the buffer.
     * The target buffer must have at least {@code size} bytes remaining.
     * This method may set the limit to the last byte read.
     * @param queue the command queue
     * @param dest the target buffer
     * @param size the size in bytes being read
     * @param offset the offset in bytes in the buffer to read from
     * @return the event indicating when the memory has been fully read into the provided buffer
     */
    public abstract Event readAsync(CommandQueue queue, ByteBuffer dest, long size, long offset);

    /**
     * Alternative version of {@link #readAsync(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long, long) },
     * sets {@code offset} to zero.
     */
    public Event readAsync(CommandQueue queue, ByteBuffer dest, long size) {
        return readAsync(queue, dest, size, 0);
    }

    /**
     * Alternative version of {@link #readAsync(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long) },
     * sets {@code size} to {@link #getSize() }
     */
    public Event readAsync(CommandQueue queue, ByteBuffer dest) {
        return readAsync(queue, dest, getSize());
    }

    /**
     * Performs a blocking write to the buffer.
     * The target buffer must have at least {@code size} bytes remaining.
     * This method may set the limit to the last byte that will be written.
     * @param queue the command queue
     * @param src the source buffer, its data is written to this buffer
     * @param size the size in bytes to write
     * @param offset the offset into the target buffer
     */
    public abstract void write(CommandQueue queue, ByteBuffer src, long size, long offset);

    /**
     * Alternative version of {@link #write(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long, long) },
     * sets {@code offset} to zero.
     */
    public void write(CommandQueue queue, ByteBuffer src, long size) {
        write(queue, src, size, 0);
    }

    /**
     * Alternative version of {@link #write(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long) },
     * sets {@code size} to {@link #getSize() }.
     */
    public void write(CommandQueue queue, ByteBuffer src) {
        write(queue, src, getSize());
    }

    /**
     * Performs an async/non-blocking write to the buffer.
     * The target buffer must have at least {@code size} bytes remaining.
     * This method may set the limit to the last byte that will be written.
     * @param queue the command queue
     * @param src the source buffer, its data is written to this buffer
     * @param size the size in bytes to write
     * @param offset the offset into the target buffer
     * @return the event object indicating when the write operation is completed
     */
    public abstract Event writeAsync(CommandQueue queue, ByteBuffer src, long size, long offset);

    /**
     * Alternative version of {@link #writeAsync(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long, long) },
     * sets {@code offset} to zero.
     */
    public Event writeAsync(CommandQueue queue, ByteBuffer src, long size) {
        return writeAsync(queue, src, size, 0);
    }

    /**
     * Alternative version of {@link #writeAsync(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer, long) },
     * sets {@code size} to {@link #getSize() }.
     */
    public Event writeAsync(CommandQueue queue, ByteBuffer src) {
        return writeAsync(queue, src, getSize());
    }

    /**
     * Performs a blocking copy operation from this buffer to the specified buffer.
     * @param queue the command queue
     * @param dest the target buffer
     * @param size the size in bytes to copy
     * @param srcOffset offset in bytes into this buffer
     * @param destOffset offset in bytes into the target buffer
     */
    public abstract void copyTo(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset);

    /**
     * Alternative version of {@link #copyTo(com.jme3.opencl.CommandQueue, com.jme3.opencl.Buffer, long, long, long) },
     * sets {@code srcOffset} and {@code destOffset} to zero.
     */
    public void copyTo(CommandQueue queue, Buffer dest, long size) {
        copyTo(queue, dest, size, 0, 0);
    }

    /**
     * Alternative version of {@link #copyTo(com.jme3.opencl.CommandQueue, com.jme3.opencl.Buffer, long) },
     * sets {@code size} to {@code this.getSize()}.
     */
    public void copyTo(CommandQueue queue, Buffer dest) {
        copyTo(queue, dest, getSize());
    }

    /**
     * Performs an async/non-blocking copy operation from this buffer to the specified buffer.
     * @param queue the command queue
     * @param dest the target buffer
     * @param size the size in bytes to copy
     * @param srcOffset offset in bytes into this buffer
     * @param destOffset offset in bytes into the target buffer
     * @return the event object indicating when the copy operation is finished
     */
    public abstract Event copyToAsync(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset);

    /**
     * Alternative version of {@link #copyToAsync(com.jme3.opencl.CommandQueue, com.jme3.opencl.Buffer, long, long, long) },
     * sets {@code srcOffset} and {@code destOffset} to zero.
     */
    public Event copyToAsync(CommandQueue queue, Buffer dest, long size) {
        return copyToAsync(queue, dest, size, 0, 0);
    }

    /**
     * Alternative version of {@link #copyToAsync(com.jme3.opencl.CommandQueue, com.jme3.opencl.Buffer, long) },
     * sets {@code size} to {@code this.getSize()}.
     */
    public Event copyToAsync(CommandQueue queue, Buffer dest) {
        return copyToAsync(queue, dest, getSize());
    }

    /**
     * Maps this buffer directly into host memory. This might be the fastest method
     * to access the contents of the buffer since the OpenCL implementation directly
     * provides the memory.<br>
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     * @param queue the command queue
     * @param size the size in bytes to map
     * @param offset the offset into this buffer
     * @param access specifies the possible access to the memory: READ_ONLY, WRITE_ONLY, READ_WRITE
     * @return the byte buffer directly reflecting the buffer contents
     */
    public abstract ByteBuffer map(CommandQueue queue, long size, long offset, MappingAccess access);

    /**
     * Alternative version of {@link #map(com.jme3.opencl.CommandQueue, long, long, com.jme3.opencl.MappingAccess) },
     * sets {@code offset} to zero.
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     */
    public ByteBuffer map(CommandQueue queue, long size, MappingAccess access) {
        return map(queue, size, 0, access);
    }

    /**
     * Alternative version of {@link #map(com.jme3.opencl.CommandQueue, long, com.jme3.opencl.MappingAccess) },
     * sets {@code size} to {@link #getSize() }.
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     */
    public ByteBuffer map(CommandQueue queue, MappingAccess access) {
        return map(queue, getSize(), access);
    }

    /**
     * Unmaps a previously mapped memory.
     * This releases the native resources and for WRITE_ONLY or READ_WRITE access,
     * the memory content is sent back to the GPU.
     * @param queue the command queue
     * @param ptr the buffer that was previously mapped
     */
    public abstract void unmap(CommandQueue queue, ByteBuffer ptr);

    /**
     * Maps this buffer asynchronously into host memory. This might be the fastest method
     * to access the contents of the buffer since the OpenCL implementation directly
     * provides the memory.<br>
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     * @param queue the command queue
     * @param size the size in bytes to map
     * @param offset the offset into this buffer
     * @param access specifies the possible access to the memory: READ_ONLY, WRITE_ONLY, READ_WRITE
     * @return the byte buffer directly reflecting the buffer contents
     * and the event indicating when the buffer contents are available
     */
    public abstract AsyncMapping mapAsync(CommandQueue queue, long size, long offset, MappingAccess access);
    /**
     * Alternative version of {@link #mapAsync(com.jme3.opencl.CommandQueue, long, long, com.jme3.opencl.MappingAccess) },
     * sets {@code offset} to zero.
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     */
    public AsyncMapping mapAsync(CommandQueue queue, long size, MappingAccess access) {
        return mapAsync(queue, size, 0, access);
    }
    /**
     * Alternative version of {@link #mapAsync(com.jme3.opencl.CommandQueue, long, com.jme3.opencl.MappingAccess) },
     * sets {@code size} to {@link #getSize() }.
     * <b>Important:</b> The mapped memory MUST be released by calling 
     * {@link #unmap(com.jme3.opencl.CommandQueue, java.nio.ByteBuffer) }.
     */
    public AsyncMapping mapAsync(CommandQueue queue, MappingAccess access) {
        return mapAsync(queue, getSize(), 0, access);
    }
    
    /**
     * Enqueues a fill operation. This method can be used to initialize or clear
     * a buffer with a certain value.
     * @param queue the command queue
     * @param pattern the buffer containing the filling pattern.
     *  The remaining bytes specify the pattern length
     * @param size the size in bytes to fill, must be a multiple of the pattern length
     * @param offset the offset in bytes into the buffer, must be a multiple of the pattern length
     * @return an event indicating when this operation is finished
     */
    public abstract Event fillAsync(CommandQueue queue, ByteBuffer pattern, long size, long offset);

    /**
     * Result of an async mapping operation, contains the event and the target byte buffer.
     * This is a work-around since no generic pair-structure is avaiable.
     *
     * @author shaman
     */
    public static class AsyncMapping {

        public final Event event;
        public final ByteBuffer buffer;

        public AsyncMapping(Event event, ByteBuffer buffer) {
            super();
            this.event = event;
            this.buffer = buffer;
        }

        /**
         * @return the event object indicating when the data in the mapped buffer
         * is available
         */
        public Event getEvent() {
            return event;
        }

        /**
         * @return the mapped buffer, only valid when the event object signals completion
         */
        public ByteBuffer getBuffer() {
            return buffer;
        }
    }
    
    /**
     * Copies this buffer to the specified image.
     * Note that no format conversion is done.
     * <br>
     * For detailed description of the origin and region paramenter, see the
     * documentation of the {@link Image} class.
     * 
     * @param queue the command queue
     * @param dest the target image
     * @param srcOffset the offset in bytes into this buffer
     * @param destOrigin the origin of the copied area
     * @param destRegion the size of the copied area
     * @return the event object
     */
    public abstract Event copyToImageAsync(CommandQueue queue, Image dest, long srcOffset, long[] destOrigin, long[] destRegion);
    
    /**
     * Aquires this buffer object for using. Only call this method if this buffer
     * represents a shared object from OpenGL, created with e.g.
     * {@link Context#bindVertexBuffer(com.jme3.scene.VertexBuffer, com.jme3.opencl.MemoryAccess) }.
     * This method must be called before the buffer is used. After the work is
     * done, the buffer must be released by calling
     * {@link #releaseBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
     * so that OpenGL can use the VertexBuffer again.
     * @param queue the command queue
     * @return the event object
     */
    public abstract Event acquireBufferForSharingAsync(CommandQueue queue);
    
    /**
     * Aquires this buffer object for using. Only call this method if this buffer
     * represents a shared object from OpenGL, created with e.g.
     * {@link Context#bindVertexBuffer(com.jme3.scene.VertexBuffer, com.jme3.opencl.MemoryAccess) }.
     * This method must be called before the buffer is used. After the work is
     * done, the buffer must be released by calling
     * {@link #releaseBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
     * so that OpenGL can use the VertexBuffer again.
     * 
     * The generated event object is directly released.
     * This brings a performance improvement when the resource is e.g. directly
     * used by a kernel afterwards on the same queue (this implicitly waits for
     * this action). If you need the event, use 
     * {@link #acquireBufferForSharingAsync(com.jme3.opencl.CommandQueue) } instead.
     * 
     * @param queue the command queue
     */
    public void acquireBufferForSharingNoEvent(CommandQueue queue) {
        //default implementation, overwrite for better performance
        acquireBufferForSharingAsync(queue).release();
    }
    
    /**
     * Releases a shared buffer object.
     * Call this method after the buffer object was acquired by
     * {@link #acquireBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
     * to hand the control back to OpenGL.
     * @param queue the command queue
     * @return the event object
     */
    public abstract Event releaseBufferForSharingAsync(CommandQueue queue);
    
    /**
     * Releases a shared buffer object.
     * Call this method after the buffer object was acquired by
     * {@link #acquireBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
     * to hand the control back to OpenGL.
     * The generated event object is directly released, resulting in 
     * performance improvements.
     * @param queue the command queue
     */
    public void releaseBufferForSharingNoEvent(CommandQueue queue) {
        //default implementation, overwrite for better performance
        releaseBufferForSharingAsync(queue).release();
    }

	@Override
	public String toString() {
		return "Buffer (" + getSize() + "B)";
	}

}
