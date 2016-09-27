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

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.opencl.*;
import com.jme3.opencl.Buffer;
import com.jme3.opencl.lwjgl.info.Info;
import java.nio.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;

/**
 *
 * @author shaman
 */
public class LwjglKernel extends Kernel {

    private final long kernel;

    public LwjglKernel(long kernel) {
        super(new ReleaserImpl(kernel));
        this.kernel = kernel;
    }

    public long getKernel() {
        return kernel;
    }
    
    @Override
    public String getName() {
        return Info.clGetKernelInfoStringASCII(kernel, CL10.CL_KERNEL_FUNCTION_NAME);
    }

    @Override
    public int getArgCount() {
        return Info.clGetKernelInfoInt(kernel, CL10.CL_KERNEL_NUM_ARGS);
    }

    @Override
    public long getMaxWorkGroupSize(Device device) {
        long d = ((LwjglDevice) device).getDevice();
        return Info.clGetKernelWorkGroupInfoPointer(kernel, d, CL10.CL_KERNEL_WORK_GROUP_SIZE);
    }
    
    @Override
    public void setArg(int index, LocalMemPerElement t) {
        int ret = CL10.clSetKernelArg (kernel, index, t.getSize() * workGroupSize.getSizes()[0] * workGroupSize.getSizes()[1] * workGroupSize.getSizes()[2]);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, LocalMem t) {
        int ret = CL10.clSetKernelArg (kernel, index, t.getSize());
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Buffer t) {
        int ret = CL10.clSetKernelArg1p(kernel, index, ((LwjglBuffer) t).getBuffer());
        Utils.checkError(ret, "clSetKernelArg");
    }
    
    @Override
    public void setArg(int index, Image i) {
        int ret = CL10.clSetKernelArg1p(kernel, index, ((LwjglImage) i).getImage());
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, byte b) {
        int ret = CL10.clSetKernelArg1b(kernel, index, b);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, short s) {
        int ret = CL10.clSetKernelArg1s(kernel, index, s);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, int i) {
        int ret = CL10.clSetKernelArg1i(kernel, index, i);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, long l) {
        int ret = CL10.clSetKernelArg1l(kernel, index, l);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, float f) {
        int ret = CL10.clSetKernelArg1f(kernel, index, f);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, double d) {
        int ret = CL10.clSetKernelArg1d(kernel, index, d);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Vector2f v) {
        int ret = CL10.clSetKernelArg2f(kernel, index, v.x, v.y);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Vector4f v) {
        int ret = CL10.clSetKernelArg4f(kernel, index, v.x, v.y, v.z, v.w);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Quaternion q) {
        int ret = CL10.clSetKernelArg4f(kernel, index, q.getX(), q.getY(), q.getZ(), q.getW());
        Utils.checkError(ret, "clSetKernelArg");
    }
    
    @Override
    public void setArg(int index, Matrix4f m) {
        FloatBuffer buf = Utils.b80f;
        buf.position(0);
        buf.limit(16);
        buf.put(m.m00).put(m.m01).put(m.m02).put(m.m03);
        buf.put(m.m10).put(m.m11).put(m.m12).put(m.m13);
        buf.put(m.m20).put(m.m21).put(m.m22).put(m.m23);
        buf.put(m.m30).put(m.m31).put(m.m32).put(m.m33);
        buf.position(0);
        int ret = CL10.clSetKernelArg(kernel, index, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, ByteBuffer buffer, long size) {
        buffer.limit((int) (buffer.position() + size));
        int ret = CL10.clSetKernelArg(kernel, index, buffer);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public Event Run(CommandQueue queue) {
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[1].put(globalWorkSize.getSizes());
        Utils.pointerBuffers[1].position(0);
        PointerBuffer p2 = null;
        if (workGroupSize.getSizes()[0] > 0) {
            p2 = Utils.pointerBuffers[2].rewind();
            p2.put(workGroupSize.getSizes());
            p2.position(0);
        }
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueNDRangeKernel(q, kernel,
			globalWorkSize.getDimension(), null, Utils.pointerBuffers[1],
			p2, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueNDRangeKernel");
        return new LwjglEvent(Utils.pointerBuffers[0].get(0));
    }
    @Override
    public void RunNoEvent(CommandQueue queue) {
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[1].put(globalWorkSize.getSizes());
        Utils.pointerBuffers[1].position(0);
        PointerBuffer p2 = null;
        if (workGroupSize.getSizes()[0] > 0) {
            p2 = Utils.pointerBuffers[2].rewind();
            p2.put(workGroupSize.getSizes());
            p2.position(0);
        }
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueNDRangeKernel(q, kernel,
			globalWorkSize.getDimension(), null, Utils.pointerBuffers[1],
			p2, null, null);
        Utils.checkError(ret, "clEnqueueNDRangeKernel");
    }

    @Override
    public ObjectReleaser getReleaser() {
        return new ReleaserImpl(kernel);
    }
    private static class ReleaserImpl implements ObjectReleaser {
        private long kernel;
        private ReleaserImpl(long kernel) {
            this.kernel = kernel;
        }
        @Override
        public void release() {
            if (kernel != 0) {
                int ret = CL10.clReleaseKernel(kernel);
                kernel = 0;
                Utils.reportError(ret, "clReleaseKernel");
            }
        }
    }
}
