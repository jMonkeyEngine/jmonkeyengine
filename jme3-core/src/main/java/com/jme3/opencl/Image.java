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

import com.jme3.math.ColorRGBA;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 * @author Sebastian Weiss
 */
public interface Image {
    
    public static enum ImageChannelType {
        SNORM_INT8,
        SNORM_INT16,
        UNORM_INT8,
        UNORM_INT16,
        UNORM_SHORT_565,
        UNORM_SHORT_555,
        UNORM_INT_101010,
        SIGNED_INT8,
        SIGNED_INT16,
        SIGNED_INT32,
        UNSIGNED_INT8,
        UNSIGNED_INT16,
        UNSIGNED_INT32,
        HALF_FLOAT,
        FLOAT
    }
    
    public static enum ImageChannelOrder {
        R, Rx, A,
        INTENSITY,
        LUMINANCE,
        RG, RGx, RA,
        RGB, RGBx,
        RGBA,
        ARGB, BGRA
    }

    public static class ImageFormat { //Struct
        public ImageChannelOrder channelOrder;
        public ImageChannelType channelType;

        public ImageFormat() {
        }

        public ImageFormat(ImageChannelOrder channelOrder, ImageChannelType channelType) {
            this.channelOrder = channelOrder;
            this.channelType = channelType;
        }

        @Override
        public String toString() {
            return "ImageFormat{" + "channelOrder=" + channelOrder + ", channelType=" + channelType + '}';
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + Objects.hashCode(this.channelOrder);
            hash = 61 * hash + Objects.hashCode(this.channelType);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ImageFormat other = (ImageFormat) obj;
            if (this.channelOrder != other.channelOrder) {
                return false;
            }
            if (this.channelType != other.channelType) {
                return false;
            }
            return true;
        }
        
    }

    public static enum ImageType {
        IMAGE_1D,
        IMAGE_1D_BUFFER,
        IMAGE_2D,
        IMAGE_3D,
        IMAGE_1D_ARRAY,
        IMAGE_2D_ARRAY
    }

    public static class ImageDescriptor { //Struct
        public ImageType type;
        public long width;
        public long height;
        public long depth;
        public long arraySize;
        public long rowPitch;
        public long slicePitch;
        /*
        public int numMipLevels;  //They must always be set to zero
        public int numSamples;
        */

        public ImageDescriptor() {
        }

        public ImageDescriptor(ImageType type, long width, long height, long depth, long arraySize, long rowPitch, long slicePitch) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.arraySize = arraySize;
            this.rowPitch = rowPitch;
            this.slicePitch = slicePitch;
        }
        public ImageDescriptor(ImageType type, long width, long height, long depth, long arraySize) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.arraySize = arraySize;
            this.rowPitch = 0;
            this.slicePitch = 0;
        }

        @Override
        public String toString() {
            return "ImageDescriptor{" + "type=" + type + ", width=" + width + ", height=" + height + ", depth=" + depth + ", arraySize=" + arraySize + ", rowPitch=" + rowPitch + ", slicePitch=" + slicePitch + '}';
        }
        
    }
    
    long getWidth();
    long getHeight();
    long getDepth();
    long getRowPitch();
    long getSlicePitch();
    long getArraySize();
    ImageFormat getImageFormat();
    ImageType getImageType();
    int getElementSize();
    
    void readImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    Event readImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    
    void writeImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    Event writeImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    
    void copyTo(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region);
    Event copyToAsync(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region);
    
    ImageMapping map(CommandQueue queue, long[] origin, long[] region, MappingAccess access);
    ImageMapping mapAsync(CommandQueue queue, long[] origin, long[] region, MappingAccess access);
    void unmap(CommandQueue queue, ImageMapping mapping);
    
    public static class ImageMapping {
        public final ByteBuffer buffer;
        public final long rowPitch;
        public final long slicePitch;
        public final Event event;

        public ImageMapping(ByteBuffer buffer, long rowPitch, long slicePitch, Event event) {
            this.buffer = buffer;
            this.rowPitch = rowPitch;
            this.slicePitch = slicePitch;
            this.event = event;
        }
        public ImageMapping(ByteBuffer buffer, long rowPitch, long slicePitch) {
            this.buffer = buffer;
            this.rowPitch = rowPitch;
            this.slicePitch = slicePitch;
            this.event = null;
        }
        
    }
    
    /**
     * Fills the image with the specified color.
     * Does <b>only</b> work if the image channel is {@link ImageChannelType#FLOAT}
     * or {@link ImageChannelType#HALF_FLOAT}.
     * @param queue
     * @param origin
     * @param region
     * @param color
     * @return 
     */
    Event fillAsync(CommandQueue queue, long[] origin, long[] region, ColorRGBA color);
    /**
     * Fills the image with the specified color given as four integer variables.
     * Does <b>not</b> work if the image channel is {@link ImageChannelType#FLOAT}
     * or {@link ImageChannelType#HALF_FLOAT}.
     * @param queue
     * @param origin
     * @param region
     * @param color
     * @return 
     */
    Event fillIntegerAsync(CommandQueue queue, long[] origin, long[] region, int[] color);
    
    Event copyToBufferAsync(CommandQueue queue, Buffer dest, long[] srcOrigin, long[] srcRegion, long destOffset);
}
