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

import com.jme3.math.ColorRGBA;
import com.jme3.opencl.*;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.gl.CLGL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shaman
 */
public class JoclImage extends Image {
    private static final Logger LOG = Logger.getLogger(JoclImage.class.getName());

    final long id;
    final CL cl;

    public JoclImage(long image) {
        super(new ReleaserImpl(image));
        this.id = image;
        this.cl = CLPlatform.getLowLevelCLInterface();
    }

    public static int decodeImageChannelOrder(ImageChannelOrder order) {
        switch (order) {
            case A:
                return CL.CL_A;
            case ARGB:
                return CL.CL_ARGB;
            case BGRA:
                return CL.CL_BGRA;
            case INTENSITY:
                return CL.CL_INTENSITY;
            case LUMINANCE:
                return CL.CL_LUMINANCE;
            case R:
                return CL.CL_R;
            case RA:
                return CL.CL_RA;
            case RG:
                return CL.CL_RG;
            case RGB:
                return CL.CL_RGB;
            case RGBA:
                return CL.CL_RGBA;
            case RGBx:
                return CL.CL_RGBx;
            case RGx:
                return CL.CL_RGx;
            case Rx:
                return CL.CL_Rx;
            default:
                throw new IllegalArgumentException("unknown image channel order: " + order);
        }
    }

    public static ImageChannelOrder encodeImageChannelOrder(int order) {
        switch (order) {
            case CL.CL_A:
                return ImageChannelOrder.A;
            case CL.CL_ARGB:
                return ImageChannelOrder.ARGB;
            case CL.CL_BGRA:
                return ImageChannelOrder.BGRA;
            case CL.CL_INTENSITY:
                return ImageChannelOrder.INTENSITY;
            case CL.CL_LUMINANCE:
                return ImageChannelOrder.LUMINANCE;
            case CL.CL_R:
                return ImageChannelOrder.R;
            case CL.CL_RA:
                return ImageChannelOrder.RA;
            case CL.CL_RG:
                return ImageChannelOrder.RG;
            case CL.CL_RGB:
                return ImageChannelOrder.RGB;
            case CL.CL_RGBA:
                return ImageChannelOrder.RGBA;
            case CL.CL_RGBx:
                return ImageChannelOrder.RGBx;
            case CL.CL_RGx:
                return ImageChannelOrder.RGx;
            case CL.CL_Rx:
                return ImageChannelOrder.Rx;
            default:
                //throw new com.jme3.opencl.OpenCLException("unknown image channel order id: " + order);
                LOG.log(Level.WARNING, "Unknown image channel order id: {0}", order);
                return null;
        }
    }

    public static int decodeImageChannelType(ImageChannelType type) {
        switch (type) {
            case FLOAT:
                return CL.CL_FLOAT;
            case HALF_FLOAT:
                return CL.CL_HALF_FLOAT;
            case SIGNED_INT16:
                return CL.CL_SIGNED_INT16;
            case SIGNED_INT32:
                return CL.CL_SIGNED_INT32;
            case SIGNED_INT8:
                return CL.CL_SIGNED_INT8;
            case SNORM_INT16:
                return CL.CL_SNORM_INT16;
            case SNORM_INT8:
                return CL.CL_SNORM_INT8;
            case UNORM_INT8:
                return CL.CL_UNORM_INT8;
            case UNORM_INT_101010:
                return CL.CL_UNORM_INT_101010;
            case UNORM_INT16:
                return CL.CL_UNORM_INT16;
            case UNORM_SHORT_565:
                return CL.CL_UNORM_SHORT_565;
            case UNORM_SHORT_555:
                return CL.CL_UNORM_SHORT_555;
            case UNSIGNED_INT16:
                return CL.CL_UNSIGNED_INT16;
            case UNSIGNED_INT32:
                return CL.CL_UNSIGNED_INT32;
            case UNSIGNED_INT8:
                return CL.CL_UNSIGNED_INT8;
            default:
                throw new IllegalArgumentException("Unknown image channel type: " + type);
        }
    }

    public static ImageChannelType encodeImageChannelType(int type) {
        switch (type) {
            case CL.CL_FLOAT:
                return ImageChannelType.FLOAT;
            case CL.CL_HALF_FLOAT:
                return ImageChannelType.HALF_FLOAT;
            case CL.CL_SIGNED_INT16:
                return ImageChannelType.SIGNED_INT16;
            case CL.CL_SIGNED_INT32:
                return ImageChannelType.SIGNED_INT32;
            case CL.CL_SIGNED_INT8:
                return ImageChannelType.SIGNED_INT8;
            case CL.CL_SNORM_INT16:
                return ImageChannelType.SNORM_INT16;
            case CL.CL_SNORM_INT8:
                return ImageChannelType.SNORM_INT8;
            case CL.CL_UNORM_INT8:
                return ImageChannelType.UNORM_INT8;
            case CL.CL_UNORM_INT16:
                return ImageChannelType.UNORM_INT16;
            case CL.CL_UNORM_INT_101010:
                return ImageChannelType.UNORM_INT_101010;
            case CL.CL_UNORM_SHORT_555:
                return ImageChannelType.UNORM_SHORT_555;
            case CL.CL_UNORM_SHORT_565:
                return ImageChannelType.UNORM_SHORT_565;
            case CL.CL_UNSIGNED_INT16:
                return ImageChannelType.UNSIGNED_INT16;
            case CL.CL_UNSIGNED_INT32:
                return ImageChannelType.UNSIGNED_INT32;
            case CL.CL_UNSIGNED_INT8:
                return ImageChannelType.UNSIGNED_INT8;
            default:
                //throw new com.jme3.opencl.OpenCLException("unknown image channel type id: " + type);
                LOG.log(Level.WARNING, "Unknown image channel type id: {0}", type);
                return null;
        }
    }

    public static int decodeImageType(ImageType type) {
        switch (type) {
//            case IMAGE_1D:
//                return CL.CL_MEM_OBJECT_IMAGE1D;
//            case IMAGE_1D_ARRAY:
//                return CL.CL_MEM_OBJECT_IMAGE1D_ARRAY;
//            case IMAGE_1D_BUFFER:
//                return CL.CL_MEM_OBJECT_IMAGE1D_BUFFER;
            case IMAGE_2D:
                return CL.CL_MEM_OBJECT_IMAGE2D;
//            case IMAGE_2D_ARRAY:
//                return CL.CL_MEM_OBJECT_IMAGE2D_ARRAY;
            case IMAGE_3D:
                return CL.CL_MEM_OBJECT_IMAGE3D;
            default:
                throw new IllegalArgumentException("Unknown or unsupported image type: " + type);
        }
    }

    public static ImageType encodeImageType(int type) {
        switch (type) {
//            case CL12.CL_MEM_OBJECT_IMAGE1D:
//                return ImageType.IMAGE_1D;
//            case CL12.CL_MEM_OBJECT_IMAGE1D_ARRAY:
//                return ImageType.IMAGE_1D_ARRAY;
//            case CL12.CL_MEM_OBJECT_IMAGE1D_BUFFER:
//                return ImageType.IMAGE_1D_BUFFER;
            case CL.CL_MEM_OBJECT_IMAGE2D:
                return ImageType.IMAGE_2D;
//            case CL12.CL_MEM_OBJECT_IMAGE2D_ARRAY:
//                return ImageType.IMAGE_2D_ARRAY;
            case CL.CL_MEM_OBJECT_IMAGE3D:
                return ImageType.IMAGE_3D;
            default:
                //throw new com.jme3.opencl.OpenCLException("Unknown image type id: " + type);
                LOG.log(Level.WARNING, "Unknown or unsupported image type with id: {0}", type);
                return null;
        }
    }

    private long getInfoSize(int param) {
        Utils.tempBuffers[0].b16l.rewind();
        int ret = cl.clGetImageInfo(id, param, 8, Utils.tempBuffers[0].b16l, null);
        Utils.checkError(ret, "clGetImageInfo");
        return Utils.tempBuffers[0].b16l.get(0);
    }
    
    @Override
    public long getWidth() {
        return getInfoSize(CL.CL_IMAGE_WIDTH);
    }

    @Override
    public long getHeight() {
        return getInfoSize(CL.CL_IMAGE_HEIGHT);
    }

    @Override
    public long getDepth() {
        return getInfoSize(CL.CL_IMAGE_DEPTH);
    }

    @Override
    public long getRowPitch() {
        return getInfoSize(CL.CL_IMAGE_ROW_PITCH);
    }

    @Override
    public long getSlicePitch() {
        return getInfoSize(CL.CL_IMAGE_SLICE_PITCH);
    }

    @Override
    public long getArraySize() {
        //return getInfoSize(CL12.CL_IMAGE_ARRAY_SIZE);
        throw new UnsupportedOperationException("Not supported in Jocl");
    }

    @Override
    public ImageFormat getImageFormat() {
        Utils.tempBuffers[0].b16i.rewind();
        int ret = cl.clGetImageInfo(id, CL.CL_IMAGE_FORMAT, 8, Utils.tempBuffers[0].b16i, null);
        Utils.checkError(ret, "clGetImageInfo");
        int channelOrder = Utils.tempBuffers[0].b16i.get(0);
        int channelType = Utils.tempBuffers[0].b16i.get(1);
        return new ImageFormat(encodeImageChannelOrder(channelOrder), encodeImageChannelType(channelType));
    }

    @Override
    public ImageType getImageType() {
        Utils.tempBuffers[0].b16i.rewind();
        int ret = cl.clGetMemObjectInfo(id, CL.CL_IMAGE_FORMAT, 5, Utils.tempBuffers[0].b16i, null);
        int type = Utils.tempBuffers[0].b16i.get(0);
        return encodeImageType(type);
    }

    @Override
    public int getElementSize() {
        return (int) getInfoSize(CL.CL_IMAGE_ELEMENT_SIZE);
    }

    @Override
    public void readImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueReadImage(q, id, CL.CL_TRUE, Utils.pointers[1], Utils.pointers[2], rowPitch, slicePitch, dest, 0, null, null);
        Utils.checkError(ret, "clEnqueueReadImage");
    }

    @Override
    public Event readImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueReadImage(q, id, CL.CL_FALSE, Utils.pointers[1], Utils.pointers[2], rowPitch, slicePitch, dest, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueReadImage");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public void writeImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueWriteImage(q, id, CL.CL_TRUE, Utils.pointers[1], Utils.pointers[2], rowPitch, slicePitch, dest, 0, null, null);
        Utils.checkError(ret, "clEnqueueWriteImage");
    }

    @Override
    public Event writeImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueWriteImage(q, id, CL.CL_FALSE, Utils.pointers[1], Utils.pointers[2], rowPitch, slicePitch, dest, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueWriteImage");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public void copyTo(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region) {
        if (srcOrigin.length!=3 || destOrigin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[3].rewind();
        Utils.pointers[1].put(srcOrigin, 0, 3).position(0);
        Utils.pointers[2].put(destOrigin, 0, 3).position(0);
        Utils.pointers[3].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueCopyImage(q, id, ((JoclImage) dest).id, Utils.pointers[1], Utils.pointers[2], Utils.pointers[3], 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyImage");
        ret = cl.clWaitForEvents(1, Utils.pointers[0]);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region) {
        if (srcOrigin.length!=3 || destOrigin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[3].rewind();
        Utils.pointers[1].put(srcOrigin, 0, 3).position(0);
        Utils.pointers[2].put(destOrigin, 0, 3).position(0);
        Utils.pointers[3].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueCopyImage(q, id, ((JoclImage) dest).id, Utils.pointers[1], Utils.pointers[2], Utils.pointers[3], 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyImage");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public ImageMapping map(CommandQueue queue, long[] origin, long[] region, MappingAccess access) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.errorBuffer.rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[3].rewind();
        Utils.pointers[4].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        long flags = Utils.getMappingAccessFlags(access);
        ByteBuffer buf = cl.clEnqueueMapImage(q, id, CL.CL_TRUE, flags, Utils.pointers[1], Utils.pointers[2], 
                Utils.pointers[3], Utils.pointers[4], 0, null, null, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        return new ImageMapping(buf, Utils.pointers[3].get(0), Utils.pointers[4].get(0));
    }

    @Override
    public ImageMapping mapAsync(CommandQueue queue, long[] origin, long[] region, MappingAccess access) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.errorBuffer.rewind();
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[3].rewind();
        Utils.pointers[4].rewind();
        Utils.pointers[1].put(origin, 0, 3).position(0);
        Utils.pointers[2].put(region, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        long flags = Utils.getMappingAccessFlags(access);
        ByteBuffer buf = cl.clEnqueueMapImage(q, id, CL.CL_FALSE, flags, Utils.pointers[1], Utils.pointers[2], 
                Utils.pointers[3], Utils.pointers[4], 0, null, Utils.pointers[0], Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        long event = Utils.pointers[0].get(0);
        return new ImageMapping(buf, Utils.pointers[3].get(0), Utils.pointers[4].get(0), 
                new JoclEvent(event));
    }

    @Override
    public void unmap(CommandQueue queue, ImageMapping mapping) {
        long q = ((JoclCommandQueue)queue).id;
        Utils.pointers[0].rewind();
        mapping.buffer.position(0);
        int ret = cl.clEnqueueUnmapMemObject(q, id, mapping.buffer, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueUnmapMemObject");
        ret = cl.clWaitForEvents(1, Utils.pointers[0]);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event fillAsync(CommandQueue queue, long[] origin, long[] region, ColorRGBA color) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        throw new UnsupportedOperationException("Not supported by Jocl!");
    }

    @Override
    public Event fillAsync(CommandQueue queue, long[] origin, long[] region, int[] color) {
        if (color.length != 4) {
            throw new IllegalArgumentException("the passed color array must have length 4");
        }
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        throw new UnsupportedOperationException("Not supported by Jocl!");
    }

    @Override
    public Event copyToBufferAsync(CommandQueue queue, Buffer dest, long[] srcOrigin, long[] srcRegion, long destOffset) {
        if (srcOrigin.length!=3 || srcRegion.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[2].rewind();
        Utils.pointers[1].put(srcOrigin, 0, 3).position(0);
        Utils.pointers[2].put(srcRegion, 0, 3).position(0);
        long q = ((JoclCommandQueue) queue).id;
        int ret = cl.clEnqueueCopyImageToBuffer(q, id, ((JoclBuffer) dest).id, 
                Utils.pointers[1], Utils.pointers[2], destOffset, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clEnqueueCopyImageToBuffer");
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }

    @Override
    public Event acquireImageForSharingAsync(CommandQueue queue) {
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueAcquireGLObjects(q, 1, Utils.pointers[1], 0, null, Utils.pointers[0]);
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }
    @Override
    public void acquireImageForSharingNoEvent(CommandQueue queue) {
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueAcquireGLObjects(q, 1, Utils.pointers[1], 0, null, null);
    }
    @Override
    public Event releaseImageForSharingAsync(CommandQueue queue) {
        Utils.pointers[0].rewind();
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, id);
        long q = ((JoclCommandQueue)queue).id;
        ((CLGL) cl).clEnqueueReleaseGLObjects(q, 1, Utils.pointers[1], 0, null, Utils.pointers[0]);
        long event = Utils.pointers[0].get(0);
        return new JoclEvent(event);
    }
    @Override
    public void releaseImageForSharingNoEvent(CommandQueue queue) {
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
