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
package com.jme3.opencl.jocl;

import com.jme3.opencl.*;
import java.nio.ByteBuffer;
import com.jogamp.opencl.*;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.gl.CLGL;

/**
 *
 * @author shaman
 */
public class JoclBuffer extends Buffer {

    final long id;
    final CL cl;

    public JoclBuffer(long id) {
        super(new ReleaserImpl(id));
        this.id = id;
        this.cl = CLPlatform.getLowLevelCLInterface();
    }
    
    @Override
    public long getSize() {
        Utils.pointers[0].rewind();
        int ret = cl.clGetMemObjectInfo(id, CL.CL_MEM_SIZE, Utils.pointers[0].elementSize(), Utils.pointers[0].getBuffer(), null);
        Utils.checkError(ret, "clGetMemObjectInfo");
        return Utils.pointers[0].get();
    }

    @Override
    public MemoryAccess getMemoryAccessFlags() {
        Utils.pointers[0].rewind();
        int ret = cl.clGetMemObjectInfo(id, CL.CL_MEM_TYPE, Utils.pointers[0].elementSize(), Utils.pointers[0].getBuffer(), null);
        Utils.checkError(ret, "clGetMemObjectInfo");
        long flags = Utils.pointers[0].get();
        return Utils.getMemoryAccessFromFlag(flags);
    }

    @Override
    public void read(CommandQueue queue, ByteBuffer dest, long size, long offset) {
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueReadBuffer(q, id, CL.CL_TRUE, offset, size, dest, 0, null, null);
        Utils.checkError(ret, "clEnqueueReadBuffer");
    }

    @Override
    public Event readAsync(CommandQueue queue, ByteBuffer dest, long size, long offset) {
        Utils.pointers[0].rewind();
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueReadBuffer(q, id, CL.CL_FALSE, offset, size, dest, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueReadBuffer");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public void write(CommandQueue queue, ByteBuffer src, long size, long offset) {
        long q = ((JoclCommandQueue)queue).id;
        int ret = cl.clEnqueueWriteBuffer(q, id, CL.CL_TRUE, offset, size, src, 0, null, null);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
    }

    @Override
    public Event writeAsync(CommandQueue queue, ByteBuffer src, long size, long offset) {
        Utils.pointers[0].rewind();
        long q = ((JoclCommandQueue)queue).id;
        int ret = cl.clEnqueueWriteBuffer(q, id, CL.CL_FALSE, offset, size, src, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueWriteBuffer");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public void copyTo(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset) {
        Utils.pointers[0].rewind();
        long q = ((JoclCommandQueue)queue).id;
        long did  = ((JoclBuffer) dest).id;
        int ret = cl.clEnqueueCopyBuffer(q, id, did, srcOffset, destOffset, size, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        ret = cl.clWaitForEvents(1, Utils.pointers[0]);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Buffer dest, long size, long srcOffset, long destOffset) {
        Utils.pointers[0].rewind();
        long q = ((JoclCommandQueue)queue).id;
        long did  = ((JoclBuffer) dest).id;
        int ret = cl.clEnqueueCopyBuffer(q, id, did, srcOffset, destOffset, size, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyBuffer");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public ByteBuffer map(CommandQueue queue, long size, long offset, MappingAccess access) {
        long q = ((JoclCommandQueue)queue).id;
        Utils.errorBuffer.rewind();
        long flags = Utils.getMappingAccessFlags(access);
        ByteBuffer b = cl.clEnqueueMapBuffer(q, id, CL.CL_TRUE, flags, offset, size, 0, null, null, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        return b;
    }

    @Override
    public void unmap(CommandQueue queue, ByteBuffer ptr) {
        long q = ((JoclCommandQueue)queue).id;
        Utils.pointers[0].rewind();
        ptr.position(0);
        int ret = cl.clEnqueueUnmapMemObject(q, id, ptr, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueUnmapMemObject");
        ret = cl.clWaitForEvents(1, Utils.pointers[0]);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public com.jme3.opencl.Buffer.AsyncMapping mapAsync(CommandQueue queue, long size, long offset, MappingAccess access) {
        long q = ((JoclCommandQueue)queue).id;
        Utils.pointers[0].rewind();
        Utils.errorBuffer.rewind();
        long flags = Utils.getMappingAccessFlags(access);
        ByteBuffer b = cl.clEnqueueMapBuffer(q, id, CL.CL_FALSE, flags, offset, size, 0, null, Utils.pointers[0], Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        long event = Utils.pointers[0].get(0);
        return new com.jme3.opencl.Buffer.AsyncMapping(new JoclEvent(event), b);
    }

    @Override
    public Event fillAsync(CommandQueue queue, ByteBuffer pattern, long size, long offset) {
        throw new UnsupportedOperationException("Not supported by Jocl!");
    }

    @Override
    public Event copyToImageAsync(CommandQueue queue, Image dest, long srcOffset, long[] destOrigin, long[] destRegion) {
        if (destOrigin.length!=3 || destRegion.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(destOrigin[0]).put(destOrigin[1]).put(destOrigin[2]).position(0);
        Utils.pointers[2].put(destRegion[0]).put(destRegion[1]).put(destRegion[2]).position(0);
        long q = ((JoclCommandQueue)queue).id;
        long i = ((JoclImage) dest).id;
        int ret = cl.clEnqueueCopyBufferToImage(q, id, i, srcOffset, Utils.pointers[1], Utils.pointers[2], 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyBufferToImage");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public Event acquireBufferForSharingAsync(CommandQueue queue) {
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueAcquireGLObjects(q, 1, Utils.pointers[1], 0, null, Utils.pointers[0]);
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }
    @Override
    public void acquireBufferForSharingNoEvent(CommandQueue queue) {
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueAcquireGLObjects(q, 1, Utils.pointers[1], 0, null, null);
    }

    @Override
    public Event releaseBufferForSharingAsync(CommandQueue queue) {
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueReleaseGLObjects(q, 1, Utils.pointers[1], 0, null, Utils.pointers[0]);
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }
    @Override
    public void releaseBufferForSharingNoEvent(CommandQueue queue) {
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueReleaseGLObjects(q, 1, Utils.pointers[1], 0, null, null);
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private long mem;
        private ReleaserImpl(long mem) {
            this.mem = mem;
        }
        @Override
        public void release() {
            if (mem != 0) {
                int ret = CLPlatform.getLowLevelCLInterface().clReleaseMemObject(mem);
                mem = 0;
                Utils.reportError(ret, "clReleaseMemObject");
            }
        }
        
    }
}
