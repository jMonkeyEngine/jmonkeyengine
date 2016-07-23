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
 * Wrapper for an OpenCL image.
 * <br>
 * An image object is similar to a {@link Buffer}, but with a specific element
 * format and buffer structure.
 * <br>
 * The image is specified by the {@link ImageDescriptor}, specifying 
 * the extend and dimension of the image, and {@link ImageFormat}, specifying
 * the type of each pixel.
 * <br>
 * An image is created from scratch using 
 * {@link Context#createImage(com.jme3.opencl.MemoryAccess, com.jme3.opencl.Image.ImageFormat, com.jme3.opencl.Image.ImageDescriptor) }
 * or from OpenGL by
 * {@link Context#bindImage(com.jme3.texture.Image, com.jme3.texture.Texture.Type, int, com.jme3.opencl.MemoryAccess) }
 * (and alternative versions).
 * 
 * <p>
 * Most methods take long arrays as input: {@code long[] origin} and {@code long[] region}.
 * Both are arrays of length 3.
 * <br>
 * <b>origin</b> defines the (x, y, z) offset in pixels in the 1D, 2D or 3D
 * image, the (x, y) offset and the image index in the 2D image array or the (x)
 * offset and the image index in the 1D image array. If image is a 2D image
 * object, origin[2] must be 0. If image is a 1D image or 1D image buffer
 * object, origin[1] and origin[2] must be 0. If image is a 1D image array
 * object, origin[2] must be 0. If image is a 1D image array object, origin[1]
 * describes the image index in the 1D image array. If image is a 2D image array
 * object, origin[2] describes the image index in the 2D image array.
 * <br>
 * <b>region</b> defines the (width, height, depth) in pixels of the 1D, 2D or
 * 3D rectangle, the (width, height) in pixels of the 2D rectangle and the
 * number of images of a 2D image array or the (width) in pixels of the 1D
 * rectangle and the number of images of a 1D image array. If image is a 2D
 * image object, region[2] must be 1. If image is a 1D image or 1D image buffer
 * object, region[1] and region[2] must be 1. If image is a 1D image array
 * object, region[2] must be 1. The values in region cannot be 0.
 *
 * @author shaman
 */
public abstract class Image extends AbstractOpenCLObject {
    
    /**
     * {@code ImageChannelType} describes the size of the channel data type.
     */
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
    
    /**
     * {@code ImageChannelOrder} specifies the number of channels and the channel layout i.e. the
memory layout in which channels are stored in the image.
     */
    public static enum ImageChannelOrder {
        R, Rx, A,
        INTENSITY,
        LUMINANCE,
        RG, RGx, RA,
        RGB, RGBx,
        RGBA,
        ARGB, BGRA
    }

    /**
     * Describes the image format, consisting of 
     * {@link ImageChannelOrder} and {@link ImageChannelType}.
     */
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

    /**
     * The possible image types / dimensions.
     */
    public static enum ImageType {
        IMAGE_1D,
        IMAGE_1D_BUFFER,
        IMAGE_2D,
        IMAGE_3D,
        IMAGE_1D_ARRAY,
        IMAGE_2D_ARRAY
    }

    /**
     * The image descriptor structure describes the type and dimensions of the image or image array.
     * <p>
     * There exists two constructors:<br>
     * {@link #ImageDescriptor(com.jme3.opencl.Image.ImageType, long, long, long, long) }
     * is used when an image with new memory should be created (used most often).<br>
     * {@link #ImageDescriptor(com.jme3.opencl.Image.ImageType, long, long, long, long, long, long, java.nio.ByteBuffer) }
     * creates an image using the provided {@code ByteBuffer} as source.
     */
    public static class ImageDescriptor { //Struct
        public ImageType type;
        public long width;
        public long height;
        public long depth;
        public long arraySize;
        public long rowPitch;
        public long slicePitch;
        public ByteBuffer hostPtr;
        /*
        public int numMipLevels;  //They must always be set to zero
        public int numSamples;
        */

        public ImageDescriptor() {
        }

        /**
         * Used to specify an image with the provided ByteBuffer as soruce
         * @param type the image type
         * @param width the width
         * @param height the height, unused for image types {@code ImageType.IMAGE_1D*}
         * @param depth the depth of the image, only used for image type {@code ImageType.IMAGE_3D}
         * @param arraySize the number of array elements for image type {@code ImageType.IMAGE_1D_ARRAY} and {@code ImageType.IMAGE_2D_ARRAY}
         * @param rowPitch the row pitch of the provided buffer
         * @param slicePitch the slice pitch of the provided buffer
         * @param hostPtr host buffer used as image memory
         */
        public ImageDescriptor(ImageType type, long width, long height, long depth, long arraySize, long rowPitch, long slicePitch, ByteBuffer hostPtr) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.arraySize = arraySize;
            this.rowPitch = rowPitch;
            this.slicePitch = slicePitch;
            this.hostPtr = hostPtr;
        }
        /**
         * Specifies an image without a host buffer, a new chunk of memory 
         * will be allocated.
         * @param type the image type
         * @param width the width
         * @param height the height, unused for image types {@code ImageType.IMAGE_1D*}
         * @param depth the depth of the image, only used for image type {@code ImageType.IMAGE_3D}
         * @param arraySize the number of array elements for image type {@code ImageType.IMAGE_1D_ARRAY} and {@code ImageType.IMAGE_2D_ARRAY}
         */
        public ImageDescriptor(ImageType type, long width, long height, long depth, long arraySize) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.arraySize = arraySize;
            this.rowPitch = 0;
            this.slicePitch = 0;
            hostPtr = null;
        }

        @Override
        public String toString() {
            return "ImageDescriptor{" + "type=" + type + ", width=" + width + ", height=" + height + ", depth=" + depth + ", arraySize=" + arraySize + ", rowPitch=" + rowPitch + ", slicePitch=" + slicePitch + '}';
        }
        
    }

    protected Image(ObjectReleaser releaser) {
        super(releaser);
    }
	
	@Override
	public Image register() {
		super.register();
		return this;
	}
	
    /**
     * @return the width of the image
     */
    public abstract long getWidth();
    /**
     * @return the height of the image
     */
    public abstract long getHeight();
    /**
     * @return the depth of the image
     */
    public abstract long getDepth();
    /**
     * @return the row pitch when the image was created from a host buffer
     * @see ImageDescriptor#ImageDescriptor(com.jme3.opencl.Image.ImageType, long, long, long, long, long, long, java.nio.ByteBuffer) 
     */
    public abstract long getRowPitch();
    /**
     * @return the slice pitch when the image was created from a host buffer
     * @see ImageDescriptor#ImageDescriptor(com.jme3.opencl.Image.ImageType, long, long, long, long, long, long, java.nio.ByteBuffer) 
     */
    public abstract long getSlicePitch();
    /**
     * @return the number of elements in the image array
     * @see ImageType#IMAGE_1D_ARRAY
     * @see ImageType#IMAGE_2D_ARRAY
     */
    public abstract long getArraySize();
    /**
     * @return the image format
     */
    public abstract ImageFormat getImageFormat();
    /**
     * @return the image type
     */
    public abstract ImageType getImageType();
    /**
     * @return the number of bytes per pixel
     */
    public abstract int getElementSize();
    
    /**
     * Performs a blocking read of the image into the specified byte buffer.
     * @param queue the command queue
     * @param dest the target byte buffer
     * @param origin the image origin location, see class description for the format
     * @param region the copied region, see class description for the format
     * @param rowPitch the row pitch of the target buffer, must be set to 0 if the image is 1D.
     * If set to 0 for 2D and 3D image, the row pitch is calculated as {@code bytesPerElement * width}
     * @param slicePitch the slice pitch of the target buffer, must be set to 0 for 1D and 2D images.
     * If set to 0 for 3D images, the slice pitch is calculated as {@code rowPitch * height}
     */
    public abstract void readImage(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    /**
     * Performs an async/non-blocking read of the image into the specified byte buffer.
     * @param queue the command queue
     * @param dest the target byte buffer
     * @param origin the image origin location, see class description for the format
     * @param region the copied region, see class description for the format
     * @param rowPitch the row pitch of the target buffer, must be set to 0 if the image is 1D.
     * If set to 0 for 2D and 3D image, the row pitch is calculated as {@code bytesPerElement * width}
     * @param slicePitch the slice pitch of the target buffer, must be set to 0 for 1D and 2D images.
     * If set to 0 for 3D images, the slice pitch is calculated as {@code rowPitch * height}
     * @return the event object indicating the status of the operation
     */
    public abstract Event readImageAsync(CommandQueue queue, ByteBuffer dest, long[] origin, long[] region, long rowPitch, long slicePitch);
    
    /**
     * Performs a blocking write from the specified byte buffer into the image.
     * @param queue the command queue
     * @param src the source buffer
     * @param origin the image origin location, see class description for the format
     * @param region the copied region, see class description for the format
     * @param rowPitch the row pitch of the target buffer, must be set to 0 if the image is 1D.
     * If set to 0 for 2D and 3D image, the row pitch is calculated as {@code bytesPerElement * width}
     * @param slicePitch the slice pitch of the target buffer, must be set to 0 for 1D and 2D images.
     * If set to 0 for 3D images, the slice pitch is calculated as {@code rowPitch * height}
     */
    public abstract void writeImage(CommandQueue queue, ByteBuffer src, long[] origin, long[] region, long rowPitch, long slicePitch);
    /**
     * Performs an async/non-blocking write from the specified byte buffer into the image.
     * @param queue the command queue
     * @param src the source buffer
     * @param origin the image origin location, see class description for the format
     * @param region the copied region, see class description for the format
     * @param rowPitch the row pitch of the target buffer, must be set to 0 if the image is 1D.
     * If set to 0 for 2D and 3D image, the row pitch is calculated as {@code bytesPerElement * width}
     * @param slicePitch the slice pitch of the target buffer, must be set to 0 for 1D and 2D images.
     * If set to 0 for 3D images, the slice pitch is calculated as {@code rowPitch * height}
     * @return the event object indicating the status of the operation
     */
    public abstract Event writeImageAsync(CommandQueue queue, ByteBuffer src, long[] origin, long[] region, long rowPitch, long slicePitch);
    
    /**
     * Performs a blocking copy operation from one image to another.
     * <b>Important:</b> Both images must have the same format!
     * @param queue the command queue
     * @param dest the target image
     * @param srcOrigin the source image origin, see class description for the format
     * @param destOrigin the target image origin, see class description for the format
     * @param region the copied region, see class description for the format
     */
    public abstract void copyTo(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region);
    /**
     * Performs an async/non-blocking copy operation from one image to another.
     * <b>Important:</b> Both images must have the same format!
     * @param queue the command queue
     * @param dest the target image
     * @param srcOrigin the source image origin, see class description for the format
     * @param destOrigin the target image origin, see class description for the format
     * @param region the copied region, see class description for the format
     * @return the event object indicating the status of the operation
     */
    public abstract Event copyToAsync(CommandQueue queue, Image dest, long[] srcOrigin, long[] destOrigin, long[] region);
    
    /**
     * Maps the image into host memory.
     * The returned structure contains the mapped byte buffer and row and slice pitch.
     * The event object is set to {@code null}, it is needed for the asnyc
     * version {@link #mapAsync(com.jme3.opencl.CommandQueue, long[], long[], com.jme3.opencl.MappingAccess) }.
     * @param queue the command queue
     * @param origin the image origin, see class description for the format
     * @param region the mapped region, see class description for the format
     * @param access the allowed memory access to the mapped memory
     * @return a structure describing the mapped memory
     * @see #unmap(com.jme3.opencl.CommandQueue, com.jme3.opencl.Image.ImageMapping) 
     */
    public abstract ImageMapping map(CommandQueue queue, long[] origin, long[] region, MappingAccess access);
    /**
     * Non-blocking version of {@link #map(com.jme3.opencl.CommandQueue, long[], long[], com.jme3.opencl.MappingAccess) }.
     * The returned structure contains the mapped byte buffer and row and slice pitch.
     * The event object is used to detect when the mapped memory is available.
     * @param queue the command queue
     * @param origin the image origin, see class description for the format
     * @param region the mapped region, see class description for the format
     * @param access the allowed memory access to the mapped memory
     * @return a structure describing the mapped memory
     * @see #unmap(com.jme3.opencl.CommandQueue, com.jme3.opencl.Image.ImageMapping) 
     */
    public abstract ImageMapping mapAsync(CommandQueue queue, long[] origin, long[] region, MappingAccess access);
    /**
     * Unmaps the mapped memory
     * @param queue the command queue
     * @param mapping the mapped memory
     */
    public abstract void unmap(CommandQueue queue, ImageMapping mapping);
    
    /**
     * Describes a mapped region of the image
     */
    public static class ImageMapping {
        /**
         * The raw byte buffer
         */
        public final ByteBuffer buffer;
        /**
         * The row pitch in bytes.
         * This value is at least {@code bytesPerElement * width}
         */
        public final long rowPitch;
        /**
         * The slice pitch in bytes.
         * This value is at least {@code rowPitch * height}
         */
        public final long slicePitch;
        /**
         * The event object used to detect when the memory is available.
         * @see #mapAsync(com.jme3.opencl.CommandQueue, long[], long[], com.jme3.opencl.MappingAccess) 
         */
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
     * @param queue the command queue
     * @param origin the image origin, see class description for the format
     * @param region the size of the region, see class description for the format
     * @param color the color to fill
     * @return an event object to detect for the completion
     */
    public abstract Event fillAsync(CommandQueue queue, long[] origin, long[] region, ColorRGBA color);
    /**
     * Fills the image with the specified color given as four integer variables.
     * Does <b>not</b> work if the image channel is {@link ImageChannelType#FLOAT}
     * or {@link ImageChannelType#HALF_FLOAT}.
     * @param queue the command queue
     * @param origin the image origin, see class description for the format
     * @param region the size of the region, see class description for the format
     * @param color the color to fill, must be an array of length 4
     * @return an event object to detect for the completion
     */
    public abstract Event fillAsync(CommandQueue queue, long[] origin, long[] region, int[] color);
    
    /**
     * Copies this image into the specified buffer, no format conversion is done.
     * This is the dual function to 
     * {@link Buffer#copyToImageAsync(com.jme3.opencl.CommandQueue, com.jme3.opencl.Image, long, long[], long[]) }.
     * @param queue the command queue
     * @param dest the target buffer
     * @param srcOrigin the image origin, see class description for the format
     * @param srcRegion the copied region, see class description for the format
     * @param destOffset an offset into the target buffer
     * @return the event object to detect the completion of the operation
     */
    public abstract Event copyToBufferAsync(CommandQueue queue, Buffer dest, long[] srcOrigin, long[] srcRegion, long destOffset);
    
    /**
     * Aquires this image object for using. Only call this method if this image
     * represents a shared object from OpenGL, created with e.g.
     * {@link Context#bindImage(com.jme3.texture.Image, com.jme3.texture.Texture.Type, int, com.jme3.opencl.MemoryAccess) }
     * or variations.
     * This method must be called before the image is used. After the work is
     * done, the image must be released by calling
     * {@link #releaseImageForSharingAsync(com.jme3.opencl.CommandQueue)  }
     * so that OpenGL can use the image/texture/renderbuffer again.
     * @param queue the command queue
     * @return the event object
     */
    public abstract Event acquireImageForSharingAsync(CommandQueue queue);
    
    /**
     * Aquires this image object for using. Only call this method if this image
     * represents a shared object from OpenGL, created with e.g.
     * {@link Context#bindImage(com.jme3.texture.Image, com.jme3.texture.Texture.Type, int, com.jme3.opencl.MemoryAccess) }
     * or variations.
     * This method must be called before the image is used. After the work is
     * done, the image must be released by calling
     * {@link #releaseImageForSharingAsync(com.jme3.opencl.CommandQueue)  }
     * so that OpenGL can use the image/texture/renderbuffer again.
     * 
     * The generated event object is directly released.
     * This brings a performance improvement when the resource is e.g. directly
     * used by a kernel afterwards on the same queue (this implicitly waits for
     * this action). If you need the event, use 
     * {@link #acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }.
     * 
     * @param queue the command queue
     */
    public void acquireImageForSharingNoEvent(CommandQueue queue) {
        //Default implementation, overwrite for performance
        acquireImageForSharingAsync(queue).release();
    }
    
    /**
     * Releases a shared image object.
     * Call this method after the image object was acquired by
     * {@link #acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * to hand the control back to OpenGL.
     * @param queue the command queue
     * @return the event object
     */
    public abstract Event releaseImageForSharingAsync(CommandQueue queue);
    
    /**
     * Releases a shared image object.
     * Call this method after the image object was acquired by
     * {@link #acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * to hand the control back to OpenGL.
     * The generated event object is directly released, resulting in 
     * performance improvements.
     * @param queue the command queue
     */
    public void releaseImageForSharingNoEvent(CommandQueue queue) {
        //default implementation, overwrite it for performance improvements
        releaseImageForSharingAsync(queue).release();
    }

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Image (");
		ImageType t = getImageType();
		str.append(t);
		str.append(", w=").append(getWidth());
		if (t == ImageType.IMAGE_2D || t == ImageType.IMAGE_3D) {
			str.append(", h=").append(getHeight());
		}
		if (t == ImageType.IMAGE_3D) {
			str.append(", d=").append(getDepth());
		}
		if (t == ImageType.IMAGE_1D_ARRAY || t == ImageType.IMAGE_2D_ARRAY) {
			str.append(", arrays=").append(getArraySize());
		}
		str.append(", ").append(getImageFormat());
		str.append(')');
		return str.toString();
	}
    
}
