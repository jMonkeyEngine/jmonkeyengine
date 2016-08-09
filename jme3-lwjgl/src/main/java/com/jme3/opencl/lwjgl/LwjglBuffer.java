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
import org.lwjgl.opencl.*;

/**
 *
 * @author shaman
 */
public class LwjglBuffer extends Buffer {

    private final CLMem buffer;

    public LwjglBuffer(CLMem buffer) {
        super(new ReleaserImpl(buffer));
        this.buffer = buffer;
    }
    public CLMem getBuffer() {
        return buffer;
    }
    
    @Override
    public long getSize() {
        return buffer.getInfoSize(CL10.CL_MEM_SIZE);
    }

    @Override
    public MemoryAccess getMemoryAccessFlags() {
        return Utils.getMemoryAccessFromFlag(buffer.getInfoLong(CL10.CL_MEM_FLAGS));
    }

    @Override
    public void read(CommandQueue queue, ByteBuffer dest, long size, long offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        dest.limit((int) (dest.position() + size));
        int ret = CL10.clEnqueueReadBuffer(((LwjglCommandQueue)queue).getQueue(), 
                buffer, CL10.CL_TRUE, offset, dest, null, null);
        Utils.checkError(ret, "clEnqueueReadBuffer");
    }

    @Override
    public Event readAsync(CommandQueue queue, ByteBuffer dest, long size, long offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        dest.limit((int) (dest.position() + size));
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueReadBuffer(q, buffer, CL10.CL_FALSE, offset, dest, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueReadBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public void write(CommandQueue queue, ByteBuffer src, long size, long offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        src.limit((int) (src.position() + size));
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueWriteBuffer(q, buffer, CL10.CL_TRUE, offset, src, null, null);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
    }

    @Override
    public Event writeAsync(CommandQueue queue, ByteBuffer src, long size, long offset) {
        //Note: LWJGL does not support the size parameter, I have to set the buffer limit
        src.limit((int) (src.position() + size));
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        int ret = CL10.clEnqueueWriteBuffer(q, buffer, CL10.CL_FALSE, offset, src, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public void copyTo(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset) {
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueCopyBuffer(q, buffer, ((LwjglBuffer) dest).buffer, srcOffset, destOffset, size, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(q.getCLEvent(event));
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset) {
        CLCommandQueue q = ((LwjglCommandQueue)queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueCopyBuffer(q, buffer, ((LwjglBuffer) dest).buffer, srcOffset, destOffset, size, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public ByteBuffer map(CommandQueue queue, long size, long offset, MappingAccess access) {
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        long flags = Utils.getMappingAccessFlags(access);
        Utils.errorBuffer.rewind();
        ByteBuffer b = CL10.clEnqueueMapBuffer(q, buffer, CL10.CL_TRUE, flags, offset, size, null, null, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        return b;
    }

    @Override
    public void unmap(CommandQueue queue, ByteBuffer ptr) {
        ptr.position(0);
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueUnmapMemObject(q, buffer, ptr, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueUnmapMemObject");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(q.getCLEvent(event));
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public com.jme3.opencl.Buffer.AsyncMapping mapAsync(CommandQueue queue, long size, long offset, MappingAccess access) {
        Utils.pointerBuffers[0].rewind();
        Utils.errorBuffer.rewind();
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        long flags = Utils.getMappingAccessFlags(access);
        ByteBuffer buf = CL10.clEnqueueMapBuffer(q, buffer, CL10.CL_FALSE, flags, offset, size, null, Utils.pointerBuffers[0], Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new com.jme3.opencl.Buffer.AsyncMapping(new LwjglEvent(q.getCLEvent(event)), buf);
    }

    @Override
    public Event fillAsync(CommandQueue queue, ByteBuffer pattern, long size, long offset) {
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL12.clEnqueueFillBuffer(q, buffer, pattern, offset, size, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueFillBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public Event copyToImageAsync(CommandQueue queue, Image dest, long srcOffset, long[] destOrigin, long[] destRegion) {
        if (destOrigin.length!=3 || destRegion.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(destOrigin).position(0);
        Utils.pointerBuffers[2].put(destRegion).position(0);
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueCopyBufferToImage(q, buffer, ((LwjglImage) dest).getImage(), 
                srcOffset, Utils.pointerBuffers[1], Utils.pointerBuffers[2], null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyBufferToImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }

    @Override
    public Event acquireBufferForSharingAsync(CommandQueue queue) {
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueAcquireGLObjects(q, buffer, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueAcquireGLObjects");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }
    @Override
    public void acquireBufferForSharingNoEvent(CommandQueue queue) {
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueAcquireGLObjects(q, buffer, null, null);
        Utils.checkError(ret, "clEnqueueAcquireGLObjects");
    }

    @Override
    public Event releaseBufferForSharingAsync(CommandQueue queue) {
        Utils.pointerBuffers[0].rewind();
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueReleaseGLObjects(q, buffer, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueReleaseGLObjects");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(q.getCLEvent(event));
    }
    @Override
    public void releaseBufferForSharingNoEvent(CommandQueue queue) {
        CLCommandQueue q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueReleaseGLObjects(q, buffer, null, null);
        Utils.checkError(ret, "clEnqueueReleaseGLObjects");
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private CLMem mem;
        private ReleaserImpl(CLMem mem) {
            this.mem = mem;
        }
        @Override
        public void release() {
            if (mem != null) {
                int ret = CL10.clReleaseMemObject(mem);
                mem = null;
                Utils.reportError(ret, "clReleaseMemObject");
            }
        }
        
    }
}
