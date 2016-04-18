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

import com.jme3.opencl.Context;
import com.jme3.opencl.KernelCompilationException;
import com.jme3.opencl.MappingAccess;
import com.jme3.opencl.MemoryAccess;
import java.nio.ByteBuffer;

/**
 *
 * @author Sebastian Weiss
 */
public class LwjglCL implements com.jme3.opencl.CL {

    @Override
    public TempBuffer[] getTempBuffers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clCreateCommandQueue(long context, long device, boolean profiling) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clReleaseCommandQueue(long queue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clCreateBuffer(long context, MemoryAccess access, int size, ByteBuffer hostPtr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueReadBuffer(long queue, long buffer, boolean blocking, int offset, int size, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueWriteBuffer(long queue, long buffer, boolean blocking, int offset, int size, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueCopyBuffer(long queue, long srcBuffer, long dstBuffer, int srcOffset, int dstOffset, int size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueFillBuffer(long queue, long buffer, ByteBuffer pattern, int patternSize, int offset, int size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueMapBuffer(long queue, long buffer, boolean blocking, MappingAccess flags, int offset, int size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clCreateImage(long context, MemoryAccess access, Context.ImageFormat format, Context.ImageDescriptor desc, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Context.ImageFormat[] clGetSupportedImageFormats(long context, MemoryAccess ma, Context.ImageType type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueReadImage(long queue, long image, boolean blocking, ByteBuffer origin, ByteBuffer region, int rowPitch, int slicePitch, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueWriteImage(long queue, long image, boolean blocking, ByteBuffer origin, ByteBuffer region, int inputRowPitch, int intputSlicePitch, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueCopyImage(long queue, long srcImage, long dstImage, ByteBuffer srcOrigin, ByteBuffer dstOrigin, ByteBuffer region) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueFillImage(long queue, long image, ByteBuffer fillColor, ByteBuffer origin, ByteBuffer region) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueCopyImageToBuffer(long queue, long srcImage, long dstBuffer, ByteBuffer srcOrigin, ByteBuffer region, int dstOffset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueCopyBufferToImage(long queue, long srcBuffer, long dstImage, int srcOffset, ByteBuffer dstOrigin, ByteBuffer region) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueMapImage(long queue, long image, boolean blocking, MappingAccess ma, ByteBuffer origin, ByteBuffer region, int rowPitch, int slicePitch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clReleaseMemObject(long mem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueUnmapMemObject(long queue, long mem, ByteBuffer ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMemSize(long mem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clCreateProgramWithSource(long context, CharSequence[] sources) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clReleaseProgram(long program) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clBuildProgram(long program, long[] devices, CharSequence optpions) throws KernelCompilationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getKernelNames(long program) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clCreateKernel(long program, String kernelName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clReleaseKernel(long kernel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clSetKernelArg(long kernel, int argIndex, int argSize, ByteBuffer argValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getKernelName(long kernel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getKernelNumArgs(long kernel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long clEnqueueNDRangeKernel(long queue, long kernel, int workDim, ByteBuffer globalWorkOffset, ByteBuffer globalWorkSize, ByteBuffer localWorkSize) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clWaitForEvents(long[] events) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clWaitForEvent(long event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEventCompleted(long event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clReleaseEvent(long event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clFlush(long queue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clFinish(long queue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
