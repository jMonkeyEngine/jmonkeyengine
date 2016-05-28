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

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.opencl.*;
import com.jme3.opencl.Buffer;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import java.nio.*;
import java.nio.charset.Charset;

import static com.jogamp.common.os.Platform.is32Bit;

/**
 *
 * @author shaman
 */
public class JoclKernel extends Kernel {

    final long kernel;
    final CL cl;

    public JoclKernel(long kernel) {
        super(new ReleaserImpl(kernel));
        this.kernel = kernel;
        this.cl = CLPlatform.getLowLevelCLInterface();
        OpenCLObjectManager.getInstance().registerObject(this);
    }
    
    @Override
    public String getName() {
        Utils.pointers[0].rewind();
        int ret = cl.clGetKernelInfo(kernel, CL.CL_KERNEL_FUNCTION_NAME, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clGetKernelInfo");
        int count = (int) Utils.pointers[0].get(0);
        ByteBuffer buf = ByteBuffer.allocateDirect(count);
        ret = cl.clGetKernelInfo(kernel, CL.CL_KERNEL_FUNCTION_NAME, count, buf, null);
        Utils.checkError(ret, "clGetKernelInfo");
        byte[] data = new byte[count];
        buf.get(data);
        return new String(data, Charset.forName("ASCII"));
    }

    @Override
    public int getArgCount() {
        Utils.tempBuffers[0].b16i.rewind();
        int ret = cl.clGetKernelInfo(kernel, CL.CL_KERNEL_NUM_ARGS, 4, Utils.tempBuffers[0].b16i, null);
        Utils.checkError(ret, "clGetKernelInfo");
        return Utils.tempBuffers[0].b16i.get(0);
    }

    @Override
    public long getMaxWorkGroupSize(Device device) {
        long d = ((JoclDevice) device).id;
        Utils.tempBuffers[0].b16l.rewind();
        int ret = cl.clGetKernelWorkGroupInfo(kernel, d, CL.CL_KERNEL_WORK_GROUP_SIZE, 8, Utils.tempBuffers[0].b16l, null);
        Utils.checkError(ret, "clGetKernelWorkGroupInfo");
        return Utils.tempBuffers[0].b16l.get(0);
   }
    
    @Override
    public void setArg(int index, LocalMemPerElement t) {
        int ret = cl.clSetKernelArg (kernel, index, t.getSize() * workGroupSize.getSizes()[0] * workGroupSize.getSizes()[1] * workGroupSize.getSizes()[2], null);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, LocalMem t) {
        int ret = cl.clSetKernelArg (kernel, index, t.getSize(), null);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Buffer t) {
        Utils.tempBuffers[0].b16l.rewind();
        Utils.tempBuffers[0].b16l.put(0, ((JoclBuffer) t).id);
        int ret = cl.clSetKernelArg(kernel, index, is32Bit()?4:8, Utils.tempBuffers[0].b16l);
        Utils.checkError(ret, "clSetKernelArg");
    }
    
    @Override
    public void setArg(int index, Image i) {
        Utils.tempBuffers[0].b16l.rewind();
        Utils.tempBuffers[0].b16l.put(0, ((JoclImage) i).id);
        int ret = cl.clSetKernelArg(kernel, index, is32Bit()?4:8, Utils.tempBuffers[0].b16l);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, byte b) {
        ByteBuffer buf = Utils.tempBuffers[0].b16;
        buf.position(0);
        buf.put(0, b);
        int ret = cl.clSetKernelArg(kernel, index, 1, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, short s) {
        ShortBuffer buf = Utils.tempBuffers[0].b16s;
        buf.position(0);
        buf.put(0, s);
        int ret = cl.clSetKernelArg(kernel, index, 2, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, int i) {
        IntBuffer buf = Utils.tempBuffers[0].b16i;
        buf.position(0);
        buf.limit(1);
        buf.put(0, i);
        int ret = cl.clSetKernelArg(kernel, index, 4, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, long l) {
        LongBuffer buf = Utils.tempBuffers[0].b16l;
        buf.position(0);
        buf.limit(1);
        buf.put(0, l);
        int ret = cl.clSetKernelArg(kernel, index, 8, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, float f) {
        FloatBuffer buf = Utils.tempBuffers[0].b16f;
        buf.position(0);
        buf.limit(1);
        buf.put(0, f);
        int ret = cl.clSetKernelArg(kernel, index, 4, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, double d) {
        DoubleBuffer buf = Utils.tempBuffers[0].b16d;
        buf.position(0);
        buf.limit(1);
        buf.put(0, d);
        int ret = cl.clSetKernelArg(kernel, index, 8, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Vector2f v) {
        FloatBuffer buf = Utils.tempBuffers[0].b16f;
        buf.position(0);
        buf.limit(2);
        buf.put(0, v.x);
        buf.put(1, v.y);
        int ret = cl.clSetKernelArg(kernel, index, 8, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Vector4f v) {
        FloatBuffer buf = Utils.tempBuffers[0].b16f;
        buf.position(0);
        buf.limit(4);
        buf.put(0, v.x);
        buf.put(1, v.y);
        buf.put(2, v.z);
        buf.put(3, v.w);
        int ret = cl.clSetKernelArg(kernel, index, 16, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, Quaternion q) {
        FloatBuffer buf = Utils.tempBuffers[0].b16f;
        buf.position(0);
        buf.limit(4);
        buf.put(0, q.getX());
        buf.put(1, q.getY());
        buf.put(2, q.getZ());
        buf.put(3, q.getW());
        int ret = cl.clSetKernelArg(kernel, index, 16, buf);
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
        int ret = cl.clSetKernelArg(kernel, index, 16*4, buf);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public void setArg(int index, ByteBuffer buffer, long size) {
        int ret = cl.clSetKernelArg(kernel, index, size, buffer);
        Utils.checkError(ret, "clSetKernelArg");
    }

    @Override
    public Event Run(CommandQueue queue) {
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(globalWorkSize.getSizes(), 0, globalWorkSize.getSizes().length);
        Utils.pointers[1].position(0);
        PointerBuffer p2 = null;
        if (workGroupSize.getSizes()[0] > 0) {
            p2 = Utils.pointers[2].rewind();
            p2.put(workGroupSize.getSizes(), 0, workGroupSize.getSizes().length);
            p2.position(0);
        }
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueNDRangeKernel(q, kernel,
			globalWorkSize.getDimension(), null, Utils.pointers[1],
			p2, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueNDRangeKernel");
        return new JoclEvent(Utils.pointers[0].get(0));
    }
    
    @Override
    public void RunNoEvent(CommandQueue queue) {
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(globalWorkSize.getSizes(), 0, globalWorkSize.getSizes().length);
        Utils.pointers[1].position(0);
        PointerBuffer p2 = null;
        if (workGroupSize.getSizes()[0] > 0) {
            p2 = Utils.pointers[2].rewind();
            p2.put(workGroupSize.getSizes(), 0, workGroupSize.getSizes().length);
            p2.position(0);
        }
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueNDRangeKernel(q, kernel,
			globalWorkSize.getDimension(), null, Utils.pointers[1],
			p2, 0, null, null);
        Utils.checkError(ret, "clEnqueueNDRangeKernel");
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private long kernel;
        private ReleaserImpl(long kernel) {
            this.kernel = kernel;
        }
        @Override
        public void release() {
            if (kernel != 0) {
                int ret = CLPlatform.getLowLevelCLInterface().clReleaseKernel(kernel);
                kernel = 0;
                Utils.reportError(ret, "clReleaseKernel");
            }
        }
    }
}
