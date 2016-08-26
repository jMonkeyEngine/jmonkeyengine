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

import com.jme3.math.ColorRGBA;
import com.jme3.opencl.*;
import com.jme3.opencl.lwjgl.info.Info;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opencl.*;

/**
 *
 * @author shaman
 */
public class LwjglImage extends Image {
    private static final Logger LOG = Logger.getLogger(LwjglImage.class.getName());

    private final long image;

    public LwjglImage(long image) {
        super(new ReleaserImpl(image));
        this.image = image;
    }

    public long getImage() {
        return image;
    }

    public static int decodeImageChannelOrder(ImageChannelOrder order) {
        switch (order) {
            case A:
                return CL10.CL_A;
            case ARGB:
                return CL10.CL_ARGB;
            case BGRA:
                return CL10.CL_BGRA;
            case INTENSITY:
                return CL10.CL_INTENSITY;
            case LUMINANCE:
                return CL10.CL_LUMINANCE;
            case R:
                return CL10.CL_R;
            case RA:
                return CL10.CL_RA;
            case RG:
                return CL10.CL_RG;
            case RGB:
                return CL10.CL_RGB;
            case RGBA:
                return CL10.CL_RGBA;
            case RGBx:
                return CL11.CL_RGBx;
            case RGx:
                return CL11.CL_RGx;
            case Rx:
                return CL11.CL_Rx;
            default:
                throw new IllegalArgumentException("unknown image channel order: " + order);
        }
    }

    public static ImageChannelOrder encodeImageChannelOrder(int order) {
        switch (order) {
            case CL10.CL_A:
                return ImageChannelOrder.A;
            case CL10.CL_ARGB:
                return ImageChannelOrder.ARGB;
            case CL10.CL_BGRA:
                return ImageChannelOrder.BGRA;
            case CL10.CL_INTENSITY:
                return ImageChannelOrder.INTENSITY;
            case CL10.CL_LUMINANCE:
                return ImageChannelOrder.LUMINANCE;
            case CL10.CL_R:
                return ImageChannelOrder.R;
            case CL10.CL_RA:
                return ImageChannelOrder.RA;
            case CL10.CL_RG:
                return ImageChannelOrder.RG;
            case CL10.CL_RGB:
                return ImageChannelOrder.RGB;
            case CL10.CL_RGBA:
                return ImageChannelOrder.RGBA;
            case CL11.CL_RGBx:
                return ImageChannelOrder.RGBx;
            case CL11.CL_RGx:
                return ImageChannelOrder.RGx;
            case CL11.CL_Rx:
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
                return CL10.CL_FLOAT;
            case HALF_FLOAT:
                return CL10.CL_HALF_FLOAT;
            case SIGNED_INT16:
                return CL10.CL_SIGNED_INT16;
            case SIGNED_INT32:
                return CL10.CL_SIGNED_INT32;
            case SIGNED_INT8:
                return CL10.CL_SIGNED_INT8;
            case SNORM_INT16:
                return CL10.CL_SNORM_INT16;
            case SNORM_INT8:
                return CL10.CL_SNORM_INT8;
            case UNORM_INT8:
                return CL10.CL_UNORM_INT8;
            case UNORM_INT_101010:
                return CL10.CL_UNORM_INT_101010;
            case UNORM_INT16:
                return CL10.CL_UNORM_INT16;
            case UNORM_SHORT_565:
                return CL10.CL_UNORM_SHORT_565;
            case UNORM_SHORT_555:
                return CL10.CL_UNORM_SHORT_555;
            case UNSIGNED_INT16:
                return CL10.CL_UNSIGNED_INT16;
            case UNSIGNED_INT32:
                return CL10.CL_UNSIGNED_INT32;
            case UNSIGNED_INT8:
                return CL10.CL_UNSIGNED_INT8;
            default:
                throw new IllegalArgumentException("Unknown image channel type: " + type);
        }
    }

    public static ImageChannelType encodeImageChannelType(int type) {
        switch (type) {
            case CL10.CL_FLOAT:
                return ImageChannelType.FLOAT;
            case CL10.CL_HALF_FLOAT:
                return ImageChannelType.HALF_FLOAT;
            case CL10.CL_SIGNED_INT16:
                return ImageChannelType.SIGNED_INT16;
            case CL10.CL_SIGNED_INT32:
                return ImageChannelType.SIGNED_INT32;
            case CL10.CL_SIGNED_INT8:
                return ImageChannelType.SIGNED_INT8;
            case CL10.CL_SNORM_INT16:
                return ImageChannelType.SNORM_INT16;
            case CL10.CL_SNORM_INT8:
                return ImageChannelType.SNORM_INT8;
            case CL10.CL_UNORM_INT8:
                return ImageChannelType.UNORM_INT8;
            case CL10.CL_UNORM_INT16:
                return ImageChannelType.UNORM_INT16;
            case CL10.CL_UNORM_INT_101010:
                return ImageChannelType.UNORM_INT_101010;
            case CL10.CL_UNORM_SHORT_555:
                return ImageChannelType.UNORM_SHORT_555;
            case CL10.CL_UNORM_SHORT_565:
                return ImageChannelType.UNORM_SHORT_565;
            case CL10.CL_UNSIGNED_INT16:
                return ImageChannelType.UNSIGNED_INT16;
            case CL10.CL_UNSIGNED_INT32:
                return ImageChannelType.UNSIGNED_INT32;
            case CL10.CL_UNSIGNED_INT8:
                return ImageChannelType.UNSIGNED_INT8;
            default:
                //throw new com.jme3.opencl.OpenCLException("unknown image channel type id: " + type);
                LOG.log(Level.WARNING, "Unknown image channel type id: {0}", type);
                return null;
        }
    }

    public static int decodeImageType(ImageType type) {
        switch (type) {
            case IMAGE_1D:
                return CL12.CL_MEM_OBJECT_IMAGE1D;
            case IMAGE_1D_ARRAY:
                return CL12.CL_MEM_OBJECT_IMAGE1D_ARRAY;
            case IMAGE_1D_BUFFER:
                return CL12.CL_MEM_OBJECT_IMAGE1D_BUFFER;
            case IMAGE_2D:
                return CL10.CL_MEM_OBJECT_IMAGE2D;
            case IMAGE_2D_ARRAY:
                return CL12.CL_MEM_OBJECT_IMAGE2D_ARRAY;
            case IMAGE_3D:
                return CL10.CL_MEM_OBJECT_IMAGE3D;
            default:
                throw new IllegalArgumentException("Unknown image type: " + type);
        }
    }

    public static ImageType encodeImageType(int type) {
        switch (type) {
            case CL12.CL_MEM_OBJECT_IMAGE1D:
                return ImageType.IMAGE_1D;
            case CL12.CL_MEM_OBJECT_IMAGE1D_ARRAY:
                return ImageType.IMAGE_1D_ARRAY;
            case CL12.CL_MEM_OBJECT_IMAGE1D_BUFFER:
                return ImageType.IMAGE_1D_BUFFER;
            case CL10.CL_MEM_OBJECT_IMAGE2D:
                return ImageType.IMAGE_2D;
            case CL12.CL_MEM_OBJECT_IMAGE2D_ARRAY:
                return ImageType.IMAGE_2D_ARRAY;
            case CL10.CL_MEM_OBJECT_IMAGE3D:
                return ImageType.IMAGE_3D;
            default:
                throw new com.jme3.opencl.OpenCLException("Unknown image type id: " + type);
        }
    }

    @Override
    public long getWidth() {
        return Info.clGetImageInfoPointer(image, CL10.CL_IMAGE_WIDTH);
    }

    @Override
    public long getHeight() {
        return Info.clGetImageInfoPointer(image, CL10.CL_IMAGE_HEIGHT);
    }

    @Override
    public long getDepth() {
        return Info.clGetImageInfoPointer(image, CL10.CL_IMAGE_DEPTH);
    }

    @Override
    public long getRowPitch() {
        return Info.clGetImageInfoPointer(image, CL10.CL_IMAGE_ROW_PITCH);
    }

    @Override
    public long getSlicePitch() {
        return Info.clGetImageInfoPointer(image, CL10.CL_IMAGE_SLICE_PITCH);
    }

    @Override
    public long getArraySize() {
        return Info.clGetImageInfoPointer(image, CL12.CL_IMAGE_ARRAY_SIZE);
    }

    @Override
    public ImageFormat getImageFormat() {
        Utils.b80.rewind();
        CLImageFormat format = new CLImageFormat(Utils.b80);
        int limit = Utils.b80.limit();
        Utils.b80.limit(format.sizeof());
        int ret = CL10.clGetImageInfo(image, CL10.CL_IMAGE_FORMAT, Utils.b80, null);
        Utils.b80.limit(limit);
        Utils.checkError(ret, "clGetImageInfo");
        return new ImageFormat(encodeImageChannelOrder(format.image_channel_order()), encodeImageChannelType(format.image_channel_data_type()));
    }

    @Override
    public ImageType getImageType() {
        int type = Info.clGetMemObjectInfoInt(image, CL10.CL_MEM_TYPE);
        return encodeImageType(type);
    }

    @Override
    public int getElementSize() {
        return Info.clGetImageInfoInt(image, CL10.CL_IMAGE_ELEMENT_SIZE);
    }

    @Override
    public void readImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueReadImage(q, image, CL10.CL_TRUE, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                rowPitch, slicePitch, dest, null, null);
        Utils.checkError(ret, "clEnqueueReadImage");
    }

    @Override
    public Event readImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueReadImage(q, image, CL10.CL_FALSE, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                rowPitch, slicePitch, dest, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueReadImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }

    @Override
    public void writeImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueWriteImage(q, image, CL10.CL_TRUE, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                rowPitch, slicePitch, dest, null, null);
        Utils.checkError(ret, "clEnqueueWriteImage");
    }

    @Override
    public Event writeImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueWriteImage(q, image, CL10.CL_FALSE, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                rowPitch, slicePitch, dest, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueWriteImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }

    @Override
    public void copyTo(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region) {
        if (srcOrigin.length!=3 || destOrigin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[3].rewind();
        Utils.pointerBuffers[1].put(srcOrigin).position(0);
        Utils.pointerBuffers[2].put(destOrigin).position(0);
        Utils.pointerBuffers[3].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueCopyImage(q, image, ((LwjglImage) dest).getImage(), 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], Utils.pointerBuffers[3], 
                null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyImage");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(event);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event copyToAsync(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region) {
        if (srcOrigin.length!=3 || destOrigin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[3].rewind();
        Utils.pointerBuffers[1].put(srcOrigin).position(0);
        Utils.pointerBuffers[2].put(destOrigin).position(0);
        Utils.pointerBuffers[3].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueCopyImage(q, image, ((LwjglImage) dest).getImage(), 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], Utils.pointerBuffers[3], 
                null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }

    @Override
    public ImageMapping map(CommandQueue queue, long[] origin, long[] region, MappingAccess access) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[3].rewind();
        Utils.pointerBuffers[4].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        long flags = Utils.getMappingAccessFlags(access);
        Utils.errorBuffer.rewind();
        ByteBuffer buf = CL10.clEnqueueMapImage(q, image, CL10.CL_TRUE, flags, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                Utils.pointerBuffers[3], Utils.pointerBuffers[4], null, null, 
                Utils.errorBuffer, null);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        return new ImageMapping(buf, Utils.pointerBuffers[3].get(0), Utils.pointerBuffers[4].get(0));
    }

    @Override
    public ImageMapping mapAsync(CommandQueue queue, long[] origin, long[] region, MappingAccess access) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[3].rewind();
        Utils.pointerBuffers[4].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        long flags = Utils.getMappingAccessFlags(access);
        Utils.errorBuffer.rewind();
        ByteBuffer buf = CL10.clEnqueueMapImage(q, image, CL10.CL_FALSE, flags, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], 
                Utils.pointerBuffers[3], Utils.pointerBuffers[4], null, Utils.pointerBuffers[0], 
                Utils.errorBuffer, null);
        Utils.checkError(Utils.errorBuffer, "clEnqueueMapBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new ImageMapping(buf, Utils.pointerBuffers[3].get(0), Utils.pointerBuffers[4].get(0), new LwjglEvent(event));
    }

    @Override
    public void unmap(CommandQueue queue, ImageMapping mapping) {
        mapping.buffer.position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        Utils.pointerBuffers[0].rewind();
        int ret = CL10.clEnqueueUnmapMemObject(q, image, mapping.buffer, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueUnmapMemObject");
        long event = Utils.pointerBuffers[0].get(0);
        ret = CL10.clWaitForEvents(event);
        Utils.checkError(ret, "clWaitForEvents");
    }

    @Override
    public Event fillAsync(CommandQueue queue, long[] origin, long[] region, ColorRGBA color) {
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        Utils.tempBuffers[0].b16f.rewind();
        Utils.tempBuffers[0].b16f.limit(4);
        Utils.tempBuffers[0].b16f.put(color.r).put(color.g).put(color.b).put(color.a);
        Utils.tempBuffers[0].b16.rewind();
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL12.clEnqueueFillImage(q, image, Utils.tempBuffers[0].b16, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueFillImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
        //TODO: why does q.getCLEvent(event) return null?
        //This is a bug in LWJGL: they forgot to include the line
        //  if ( __result == CL_SUCCESS ) command_queue.registerCLEvent(event);
        // after the native call
    }

    @Override
    public Event fillAsync(CommandQueue queue, long[] origin, long[] region, int[] color) {
        if (color.length != 4) {
            throw new IllegalArgumentException("the passed color array must have length 4");
        }
        if (origin.length!=3 || region.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(origin).position(0);
        Utils.pointerBuffers[2].put(region).position(0);
        Utils.tempBuffers[0].b16i.rewind();
        Utils.tempBuffers[0].b16i.limit(4);
        Utils.tempBuffers[0].b16i.put(color);
        Utils.tempBuffers[0].b16.rewind();
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL12.clEnqueueFillImage(q, image, Utils.tempBuffers[0].b16, 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueFillImage");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }

    @Override
    public Event copyToBufferAsync(CommandQueue queue, Buffer dest, long[] srcOrigin, long[] srcRegion, long destOffset) {
        if (srcOrigin.length!=3 || srcRegion.length!=3) {
            throw new IllegalArgumentException("origin and region must both be arrays of length 3");
        }
        Utils.pointerBuffers[0].rewind();
        Utils.pointerBuffers[1].rewind();
        Utils.pointerBuffers[2].rewind();
        Utils.pointerBuffers[1].put(srcOrigin).position(0);
        Utils.pointerBuffers[2].put(srcRegion).position(0);
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10.clEnqueueCopyImageToBuffer(q, image, ((LwjglBuffer) dest).getBuffer(), 
                Utils.pointerBuffers[1], Utils.pointerBuffers[2], destOffset, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueCopyImageToBuffer");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }

    @Override
    public Event acquireImageForSharingAsync(CommandQueue queue) {
        Utils.pointerBuffers[0].rewind();
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueAcquireGLObjects(q, image, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueAcquireGLObjects");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }
    @Override
    public void acquireImageForSharingNoEvent(CommandQueue queue) {
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueAcquireGLObjects(q, image, null, null);
        Utils.checkError(ret, "clEnqueueAcquireGLObjects");
    }
    @Override
    public Event releaseImageForSharingAsync(CommandQueue queue) {
        Utils.assertSharingPossible();
        Utils.pointerBuffers[0].rewind();
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueReleaseGLObjects(q, image, null, Utils.pointerBuffers[0]);
        Utils.checkError(ret, "clEnqueueReleaseGLObjects");
        long event = Utils.pointerBuffers[0].get(0);
        return new LwjglEvent(event);
    }
    @Override
    public void releaseImageForSharingNoEvent(CommandQueue queue) {
        Utils.assertSharingPossible();
        long q = ((LwjglCommandQueue) queue).getQueue();
        int ret = CL10GL.clEnqueueReleaseGLObjects(q, image, null, null);
        Utils.checkError(ret, "clEnqueueReleaseGLObjects");
    }
    
    private static class ReleaserImpl implements ObjectReleaser {
        private long mem;
        private ReleaserImpl(long mem) {
            this.mem = mem;
        }
        @Override
        public void release() {
            if (mem != 0) {
                int ret = CL10.clReleaseMemObject(mem);
                mem = 0;
                Utils.reportError(ret, "clReleaseMemObject");
            }
        }
        
    }
}
