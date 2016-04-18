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

import java.nio.*;

/**
 * Interface for OpenCL implementations
 *
 * @author Sebastian Weiss
 */
public interface CL {

    //temp buffers for argument passing
    public static class TempBuffer {

        /**
         * 16-Bytes (4 floats) of a byte buffer
         */
        public final ByteBuffer b16;
        /**
         * Short-buffer view on b16
         */
        public final ShortBuffer b16s;
        /**
         * int buffer view on b16
         */
        public final IntBuffer b16i;
        /**
         * long buffer view on b16
         */
        public final LongBuffer b16l;
        /**
         * float buffer view on b16
         */
        public final FloatBuffer b16f;
        /**
         * double buffer view on b16
         */
        public final DoubleBuffer b16d;

        public TempBuffer(ByteBuffer b16) {
            this.b16 = b16;
            b16.rewind();
            this.b16s = b16.asShortBuffer();
            this.b16i = b16.asIntBuffer();
            this.b16l = b16.asLongBuffer();
            this.b16f = b16.asFloatBuffer();
            this.b16d = b16.asDoubleBuffer();
        }
    }

    /**
     * Returns up to 4 temp buffer instances
     *
     * @return
     */
    TempBuffer[] getTempBuffers();

    //entry point
    long getContext();

    //OpenCL functions
    long clCreateCommandQueue(long context, long device, boolean profiling);

    void clReleaseCommandQueue(long queue);

    long clCreateBuffer(long context, MemoryAccess access, int size, ByteBuffer hostPtr);

    long clEnqueueReadBuffer(long queue, long buffer, boolean blocking, int offset, int size, ByteBuffer ptr);

    long clEnqueueWriteBuffer(long queue, long buffer, boolean blocking, int offset, int size, ByteBuffer ptr);

    long clEnqueueCopyBuffer(long queue, long srcBuffer, long dstBuffer, int srcOffset, int dstOffset, int size);

    long clEnqueueFillBuffer(long queue, long buffer, ByteBuffer pattern, int patternSize, int offset, int size);

    long clEnqueueMapBuffer(long queue, long buffer, boolean blocking, MappingAccess flags, int offset, int size);

    long clCreateImage(long context, MemoryAccess access, Context.ImageFormat format, Context.ImageDescriptor desc, ByteBuffer ptr);

    Context.ImageFormat[] clGetSupportedImageFormats(long context, MemoryAccess ma, Context.ImageType type);

    long clEnqueueReadImage(long queue, long image, boolean blocking, ByteBuffer origin, ByteBuffer region, int rowPitch, int slicePitch, ByteBuffer ptr);

    long clEnqueueWriteImage(long queue, long image, boolean blocking, ByteBuffer origin, ByteBuffer region, int inputRowPitch, int intputSlicePitch, ByteBuffer ptr);

    long clEnqueueCopyImage(long queue, long srcImage, long dstImage, ByteBuffer srcOrigin, ByteBuffer dstOrigin, ByteBuffer region);

    long clEnqueueFillImage(long queue, long image, ByteBuffer fillColor, ByteBuffer origin, ByteBuffer region);

    long clEnqueueCopyImageToBuffer(long queue, long srcImage, long dstBuffer, ByteBuffer srcOrigin, ByteBuffer region, int dstOffset);

    long clEnqueueCopyBufferToImage(long queue, long srcBuffer, long dstImage, int srcOffset, ByteBuffer dstOrigin, ByteBuffer region);

    long clEnqueueMapImage(long queue, long image, boolean blocking, MappingAccess ma, ByteBuffer origin, ByteBuffer region, int rowPitch, int slicePitch);
	//TODO: clGetImageInfo

    void clReleaseMemObject(long mem);

    long clEnqueueUnmapMemObject(long queue, long mem, ByteBuffer ptr);

    int getMemSize(long mem); //uses clGetMemObjectInfo

    long clCreateProgramWithSource(long context, CharSequence[] sources);

    //TODO: create from binary

    long clReleaseProgram(long program);

    void clBuildProgram(long program, long[] devices, CharSequence optpions) throws KernelCompilationException;

    String getKernelNames(long program); //uses clGetProgramInfo

    long clCreateKernel(long program, String kernelName);

    void clReleaseKernel(long kernel);

    void clSetKernelArg(long kernel, int argIndex, int argSize, ByteBuffer argValue);

    String getKernelName(long kernel); //uses clGetKernelInfo

    int getKernelNumArgs(long kernel); //uses clGetKernelInfo
    //TODO: clGetKernelWorkGroupInfo

    long clEnqueueNDRangeKernel(long queue, long kernel, int workDim,
            ByteBuffer globalWorkOffset, ByteBuffer globalWorkSize, ByteBuffer localWorkSize);

    void clWaitForEvents(long[] events);

    void clWaitForEvent(long event);

    boolean isEventCompleted(long event); //uses clGetEventInfo

    void clReleaseEvent(long event);

    void clFlush(long queue);

    void clFinish(long queue);

}
