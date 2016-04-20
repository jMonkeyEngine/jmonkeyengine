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
package com.jme3.opencl.lwjgl;

import com.jme3.opencl.*;
import java.nio.ByteBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLEvent;
import org.lwjgl.opencl.CLMem;

/**
 *
 * @author Sebastian Weiss
 */
public class LwjglBuffer implements Buffer {

    private final CLMem buffer;

    public LwjglBuffer(CLMem buffer) {
        this.buffer = buffer;
    }
    public CLMem getBuffer() {
        return buffer;
    }
    
    @Override
    public int getSize() {
        return buffer.getInfoInt(CL10.CL_MEM_SIZE);
    }

    @Override
    public MemoryAccess getMemoryAccessFlags() {
        return Utils.getMemoryAccessFromFlag(buffer.getInfoLong(CL10.CL_MEM_FLAGS));
    }

    @Override
    public void read(CommandQueue queue, ByteBuffer dest, int size, int offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        dest.limit(dest.position() + size);
        int ret = CL10.clEnqueueReadBuffer(((LwjglCommandQueue)queue).getQueue(), 
                buffer, CL10.CL_TRUE, offset, dest, null, null);
        Utils.checkError(ret, "clEnqueueReadBuffer");
    }

    @Override
    public void read(CommandQueue queue, ByteBuffer dest, int size) {
        read(queue, dest, size, 0);
    }

    @Override
    public void read(CommandQueue queue, ByteBuffer dest) {
        read(queue, dest, getSize());
    }

    @Override
    public Event readAsync(CommandQueue queue, ByteBuffer dest, int size, int offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        dest.limit(dest.position() + size);
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueReadBuffer(q, buffer, CL10.CL_FALSE, offset, dest, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueReadBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public Event readAsync(CommandQueue queue, ByteBuffer dest, int size) {
        return readAsync(queue, dest, size, 0);
    }

    @Override
    public Event readAsync(CommandQueue queue, ByteBuffer dest) {
        return readAsync(queue, dest, getSize());
    }

    @Override
    public void write(CommandQueue queue, ByteBuffer src, int size, int offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        src.limit(src.position() + size);
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueWriteBuffer(q, buffer, CL10.CL_TRUE, offset, src, null, null);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
    }

    @Override
    public void write(CommandQueue queue, ByteBuffer src, int size) {
        write(queue, src, size, 0);
    }

    @Override
    public void write(CommandQueue queue, ByteBuffer src) {
        write(queue, src, getSize());
    }

    @Override
    public Event writeAsync(CommandQueue queue, ByteBuffer src, int size, int offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        src.limit(src.position() + size);
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueWriteBuffer(q, buffer, CL10.CL_FALSE, offset, src, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public Event writeAsync(CommandQueue queue, ByteBuffer src, int size) {
        return writeAsync(queue, src, size, 0);
    }

    @Override
    public Event writeAsync(CommandQueue queue, ByteBuffer src) {
        return writeAsync(queue, src, getSize());
    }

    @Override
    public void copyTo(CommandQueue queue, Buffer dest, int size, int srcOffset, int destOffset) {
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueCopyBuffer(q, buffer, ((LwjglBuffer) dest).buffer, srcOffset, destOffset, size, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(q.getCLEvent(event));
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public void copyTo(CommandQueue queue, Buffer dest, int size) {
        copyTo(queue, dest, size, 0, 0);
    }

    @Override
    public void copyTo(CommandQueue queue, Buffer dest) {
        copyTo(queue, dest, getSize());
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Buffer dest, int size, int srcOffset, int destOffset) {
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueCopyBuffer(q, buffer, ((LwjglBuffer) dest).buffer, srcOffset, destOffset, size, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Buffer dest, int size) {
        return copyToAsync(queue, dest, size, 0, 0);
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Buffer dest) {
        return copyToAsync(queue, dest, getSize());
    }

    @Override
    public ByteBuffer map(CommandQueue queue, int size, int offset, MappingAccess access) {
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        long flags = Utils.getMappingAccessFlags(access);
        Utils.errorBuffer.rewind();
        ByteBuffer b = CL10.clEnqueueMapBuffer(q, buffer, CL10.CL_TRUE, flags, offset, size, null, null, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        return b;
    }

    @Override
    public ByteBuffer map(CommandQueue queue, int size, MappingAccess access) {
        return map(queue, size, 0, access);
    }

    @Override
    public ByteBuffer map(CommandQueue queue, MappingAccess access) {
        return map(queue, getSize(), access);
    }

    @Override
    public void unmap(CommandQueue queue, ByteBuffer ptr) {
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueUnmapMemObject(q, buffer, ptr, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueUnmapMemObject");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(q.getCLEvent(event));
        Utils.checkError(ret, "clWaitForEvents");
    }
    
}
