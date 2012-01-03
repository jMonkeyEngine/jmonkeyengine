/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.texture;

import com.jme3.export.*;
import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>Image</code> defines a data format for a graphical image. The image
 * is defined by a format, a height and width, and the image data. The width and
 * height must be greater than 0. The data is contained in a byte buffer, and
 * should be packed before creation of the image object.
 *
 * @author Mark Powell
 * @author Joshua Slack
 * @version $Id: Image.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
public class Image extends NativeObject implements Savable /*, Cloneable*/ {

    public enum Format {
        /**
         * 8-bit alpha
         */
        Alpha8(8),
        
        /**
         * 16-bit alpha
         */
        Alpha16(16),

        /**
         * 8-bit grayscale/luminance.
         */
        Luminance8(8),
        
        /**
         * 16-bit grayscale/luminance.
         */
        Luminance16(16),
        
        /**
         * half-precision floating-point grayscale/luminance.
         */
        Luminance16F(16,true),
        
        /**
         * single-precision floating-point grayscale/luminance.
         */
        Luminance32F(32,true),
        
        /**
         * 8-bit luminance/grayscale and 8-bit alpha.
         */
        Luminance8Alpha8(16),
        
        /**
         * 16-bit luminance/grayscale and 16-bit alpha.
         */
        Luminance16Alpha16(32),
        
        /**
         * half-precision floating-point grayscale/luminance and alpha.
         */
        Luminance16FAlpha16F(32,true),

        Intensity8(8),
        Intensity16(16),

        /**
         * 8-bit blue, green, and red.
         */
        BGR8(24), // BGR and ABGR formats are often used on windows systems
        
        /**
         * 8-bit red, green, and blue.
         */
        RGB8(24),
        
        /**
         * 10-bit red, green, and blue.
         */
        RGB10(30),
        
        /**
         * 16-bit red, green, and blue.
         */
        RGB16(48),

        /**
         * 5-bit red, 6-bit green, and 5-bit blue.
         */
        RGB565(16),
        
        /**
         * 4-bit alpha, red, green, and blue. Used on Android only.
         */
        ARGB4444(16),
        
        /**
         * 5-bit red, green, and blue with 1-bit alpha.
         */
        RGB5A1(16),
        
        /**
         * 8-bit red, green, blue, and alpha.
         */
        RGBA8(32),
        
        /**
         * 8-bit alpha, blue, green, and red.
         */
        ABGR8(32),
        
        /**
         * 16-bit red, green, blue and alpha
         */
        RGBA16(64),

        /**
         * S3TC compression DXT1. 
         * Called BC1 in DirectX10.
         */
        DXT1(4,false,true, false),
        
        /**
         * S3TC compression DXT1 with 1-bit alpha.
         */
        DXT1A(4,false,true, false),
        
        /**
         * S3TC compression DXT3 with 4-bit alpha.
         * Called BC2 in DirectX10.
         */
        DXT3(8,false,true, false),
        
        /**
         * S3TC compression DXT5 with interpolated 8-bit alpha.
         * Called BC3 in DirectX10.
         */
        DXT5(8,false,true, false),
        
        /**
         * Luminance-Alpha Texture Compression. 
         * Called BC5 in DirectX10.
         */
        LATC(8, false, true, false),

        /**
         * Arbitrary depth format. The precision is chosen by the video
         * hardware.
         */
        Depth(0,true,false,false),
        
        /**
         * 16-bit depth.
         */
        Depth16(16,true,false,false),
        
        /**
         * 24-bit depth.
         */
        Depth24(24,true,false,false),
        
        /**
         * 32-bit depth.
         */
        Depth32(32,true,false,false),
        
        /**
         * single-precision floating point depth.
         */
        Depth32F(32,true,false,true),

        /**
         * Texture data is stored as {@link Format#RGB16F} in system memory,
         * but will be converted to {@link Format#RGB111110F} when sent
         * to the video hardware.
         */
        RGB16F_to_RGB111110F(48,true),
        
        /**
         * unsigned floating-point red, green and blue that uses 32 bits.
         */
        RGB111110F(32,true),
        
        /**
         * Texture data is stored as {@link Format#RGB16F} in system memory,
         * but will be converted to {@link Format#RGB9E5} when sent
         * to the video hardware.
         */
        RGB16F_to_RGB9E5(48,true),
        
        /**
         * 9-bit red, green and blue with 5-bit exponent.
         */
        RGB9E5(32,true),
        
        /**
         * half-precision floating point red, green, and blue.
         */
        RGB16F(48,true),
        
        /**
         * half-precision floating point red, green, blue, and alpha.
         */
        RGBA16F(64,true),
        
        /**
         * single-precision floating point red, green, and blue.
         */
        RGB32F(96,true),
        
        /**
         * single-precision floating point red, green, blue and alpha.
         */
        RGBA32F(128,true),

        /**
         * Luminance/grayscale texture compression. 
         * Called BC4 in DirectX10.
         */
        LTC(4, false, true, false);

        private int bpp;
        private boolean isDepth;
        private boolean isCompressed;
        private boolean isFloatingPoint;

        private Format(int bpp){
            this.bpp = bpp;
        }

        private Format(int bpp, boolean isFP){
            this(bpp);
            this.isFloatingPoint = isFP;
        }

        private Format(int bpp, boolean isDepth, boolean isCompressed, boolean isFP){
            this(bpp, isFP);
            this.isDepth = isDepth;
            this.isCompressed = isCompressed;
        }

        /**
         * @return bits per pixel.
         */
        public int getBitsPerPixel(){
            return bpp;
        }

        /**
         * @return True if this format is a depth format, false otherwise.
         */
        public boolean isDepthFormat(){
            return isDepth;
        }

        /**
         * @return True if this is a compressed image format, false if
         * uncompressed.
         */
        public boolean isCompressed() {
            return isCompressed;
        }

        /**
         * @return True if this image format is in floating point, 
         * false if it is an integer format.
         */
        public boolean isFloatingPont(){
            return isFloatingPoint;
        }

    }

    // image attributes
    protected Format format;
    protected int width, height, depth;
    protected int[] mipMapSizes;
    protected ArrayList<ByteBuffer> data;
    protected transient Object efficientData;
    protected int multiSamples = 1;
//    protected int mipOffset = 0;

    @Override
    public void resetObject() {
        this.id = -1;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((Renderer)rendererObject).deleteImage(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new Image(id);
    }

    /**
     * @return A shallow clone of this image. The data is not cloned.
     */
    @Override
    public Image clone(){
        Image clone = (Image) super.clone();
        clone.mipMapSizes = mipMapSizes != null ? mipMapSizes.clone() : null;
        clone.data = data != null ? new ArrayList<ByteBuffer>(data) : null;
        clone.setUpdateNeeded();
        return clone;
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. All values
     * are undefined.
     */
    public Image() {
        super(Image.class);
        data = new ArrayList<ByteBuffer>(1);
    }

    protected Image(int id){
        super(Image.class, id);
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The
     * attributes of the image are defined during construction.
     *
     * @param format
     *            the data format of the image.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data.
     * @param mipMapSizes
     *            the array of mipmap sizes, or null for no mipmaps.
     */
    public Image(Format format, int width, int height, int depth, ArrayList<ByteBuffer> data,
            int[] mipMapSizes) {
        
        this();

        if (mipMapSizes != null && mipMapSizes.length <= 1) {
            mipMapSizes = null;
        }

        setFormat(format);
        this.width = width;
        this.height = height;
        this.data = data;
        this.depth = depth;
        this.mipMapSizes = mipMapSizes;
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The
     * attributes of the image are defined during construction.
     *
     * @param format
     *            the data format of the image.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data.
     * @param mipMapSizes
     *            the array of mipmap sizes, or null for no mipmaps.
     */
    public Image(Format format, int width, int height, ByteBuffer data,
            int[] mipMapSizes) {

        this();

        if (mipMapSizes != null && mipMapSizes.length <= 1) {
            mipMapSizes = null;
        }

        setFormat(format);
        this.width = width;
        this.height = height;
        if (data != null){
            this.data = new ArrayList<ByteBuffer>(1);
            this.data.add(data);
        }
        this.mipMapSizes = mipMapSizes;
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The
     * attributes of the image are defined during construction.
     *
     * @param format
     *            the data format of the image.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data.
     */
    public Image(Format format, int width, int height, int depth, ArrayList<ByteBuffer> data) {
        this(format, width, height, depth, data, null);
    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The
     * attributes of the image are defined during construction.
     *
     * @param format
     *            the data format of the image.
     * @param width
     *            the width of the image.
     * @param height
     *            the height of the image.
     * @param data
     *            the image data.
     */
    public Image(Format format, int width, int height, ByteBuffer data) {
        this(format, width, height, data, null);
    }

    /**
     * @return The number of samples (for multisampled textures).
     * @see Image#setMultiSamples(int)
     */
    public int getMultiSamples() {
        return multiSamples;
    }

    /**
     * @param multiSamples Set the number of samples to use for this image,
     * setting this to a value higher than 1 turns this image/texture
     * into a multisample texture (on OpenGL3.1 and higher).
     */
    public void setMultiSamples(int multiSamples) {
        if (multiSamples <= 0)
            throw new IllegalArgumentException("multiSamples must be > 0");

        if (getData(0) != null)
            throw new IllegalArgumentException("Cannot upload data as multisample texture");

        if (hasMipmaps())
            throw new IllegalArgumentException("Multisample textures do not support mipmaps");

        this.multiSamples = multiSamples;
    }

    /**
     * <code>setData</code> sets the data that makes up the image. This data
     * is packed into an array of <code>ByteBuffer</code> objects.
     *
     * @param data
     *            the data that contains the image information.
     */
    public void setData(ArrayList<ByteBuffer> data) {
        this.data = data;
        setUpdateNeeded();
    }

    /**
     * <code>setData</code> sets the data that makes up the image. This data
     * is packed into a single <code>ByteBuffer</code>.
     *
     * @param data
     *            the data that contains the image information.
     */
    public void setData(ByteBuffer data) {
        this.data = new ArrayList<ByteBuffer>(1);
        this.data.add(data);
        setUpdateNeeded();
    }

    public void addData(ByteBuffer data) {
        if (this.data == null)
            this.data = new ArrayList<ByteBuffer>(1);
        this.data.add(data);
        setUpdateNeeded();
    }

    public void setData(int index, ByteBuffer data) {
        if (index >= 0) {
            while (this.data.size() <= index) {
                this.data.add(null);
            }
            this.data.set(index, data);
            setUpdateNeeded();
        } else {
            throw new IllegalArgumentException("index must be greater than or equal to 0.");
        }
    }

    /**
     * Set the efficient data representation of this image.
     * <p>
     * Some system implementations are more efficient at operating
     * on data other than ByteBuffers, in that case, this method can be used.
     *
     * @param efficientData
     */
    public void setEfficentData(Object efficientData){
        this.efficientData = efficientData;
        setUpdateNeeded();
    }

    /**
     * @return The efficient data representation of this image.
     * @see Image#setEfficentData(java.lang.Object)
     */
    public Object getEfficentData(){
        return efficientData;
    }

    /**
     * Sets the mipmap sizes stored in this image's data buffer. Mipmaps are
     * stored sequentially, and the first mipmap is the main image data. To
     * specify no mipmaps, pass null and this will automatically be expanded
     * into a single mipmap of the full
     *
     * @param mipMapSizes
     *            the mipmap sizes array, or null for a single image map.
     */
    public void setMipMapSizes(int[] mipMapSizes) {
        if (mipMapSizes != null && mipMapSizes.length <= 1)
            mipMapSizes = null;

        this.mipMapSizes = mipMapSizes;
        setUpdateNeeded();
    }

    /**
     * <code>setHeight</code> sets the height value of the image. It is
     * typically a good idea to try to keep this as a multiple of 2.
     *
     * @param height
     *            the height of the image.
     */
    public void setHeight(int height) {
        this.height = height;
        setUpdateNeeded();
    }

    /**
     * <code>setDepth</code> sets the depth value of the image. It is
     * typically a good idea to try to keep this as a multiple of 2. This is
     * used for 3d images.
     *
     * @param depth
     *            the depth of the image.
     */
    public void setDepth(int depth) {
        this.depth = depth;
        setUpdateNeeded();
    }

    /**
     * <code>setWidth</code> sets the width value of the image. It is
     * typically a good idea to try to keep this as a multiple of 2.
     *
     * @param width
     *            the width of the image.
     */
    public void setWidth(int width) {
        this.width = width;
        setUpdateNeeded();
    }

    /**
     * <code>setFormat</code> sets the image format for this image.
     *
     * @param format
     *            the image format.
     * @throws NullPointerException
     *             if format is null
     * @see Format
     */
    public void setFormat(Format format) {
        if (format == null) {
            throw new NullPointerException("format may not be null.");
        }

        this.format = format;
        setUpdateNeeded();
    }

    /**
     * <code>getFormat</code> returns the image format for this image.
     *
     * @return the image format.
     * @see Format
     */
    public Format getFormat() {
        return format;
    }

    /**
     * <code>getWidth</code> returns the width of this image.
     *
     * @return the width of this image.
     */
    public int getWidth() {
        return width;
    }

    /**
     * <code>getHeight</code> returns the height of this image.
     *
     * @return the height of this image.
     */
    public int getHeight() {
        return height;
    }

    /**
     * <code>getDepth</code> returns the depth of this image (for 3d images).
     *
     * @return the depth of this image.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * <code>getData</code> returns the data for this image. If the data is
     * undefined, null will be returned.
     *
     * @return the data for this image.
     */
    public List<ByteBuffer> getData() {
        return data;
    }

    /**
     * <code>getData</code> returns the data for this image. If the data is
     * undefined, null will be returned.
     *
     * @return the data for this image.
     */
    public ByteBuffer getData(int index) {
        if (data.size() > index)
            return data.get(index);
        else
            return null;
    }

    /**
     * Returns whether the image data contains mipmaps.
     *
     * @return true if the image data contains mipmaps, false if not.
     */
    public boolean hasMipmaps() {
        return mipMapSizes != null;
    }

    /**
     * Returns the mipmap sizes for this image.
     *
     * @return the mipmap sizes for this image.
     */
    public int[] getMipMapSizes() {
        return mipMapSizes;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[size=").append(width).append("x").append(height);

        if (depth > 1)
            sb.append("x").append(depth);

        sb.append(", format=").append(format.name());

        if (hasMipmaps())
            sb.append(", mips");

        if (getId() >= 0)
            sb.append(", id=").append(id);

        sb.append("]");
        
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Image)) {
            return false;
        }
        Image that = (Image) other;
        if (this.getFormat() != that.getFormat())
            return false;
        if (this.getWidth() != that.getWidth())
            return false;
        if (this.getHeight() != that.getHeight())
            return false;
        if (this.getData() != null && !this.getData().equals(that.getData()))
            return false;
        if (this.getData() == null && that.getData() != null)
            return false;
        if (this.getMipMapSizes() != null
                && !Arrays.equals(this.getMipMapSizes(), that.getMipMapSizes()))
            return false;
        if (this.getMipMapSizes() == null && that.getMipMapSizes() != null)
            return false;
        if (this.getMultiSamples() != that.getMultiSamples())
            return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.format != null ? this.format.hashCode() : 0);
        hash = 97 * hash + this.width;
        hash = 97 * hash + this.height;
        hash = 97 * hash + this.depth;
        hash = 97 * hash + Arrays.hashCode(this.mipMapSizes);
        hash = 97 * hash + (this.data != null ? this.data.hashCode() : 0);
        hash = 97 * hash + this.multiSamples;
        return hash;
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(format, "format", Format.RGBA8);
        capsule.write(width, "width", 0);
        capsule.write(height, "height", 0);
        capsule.write(depth, "depth", 0);
        capsule.write(mipMapSizes, "mipMapSizes", null);
        capsule.write(multiSamples, "multiSamples", 1);
        capsule.writeByteBufferArrayList(data, "data", null);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        format = capsule.readEnum("format", Format.class, Format.RGBA8);
        width = capsule.readInt("width", 0);
        height = capsule.readInt("height", 0);
        depth = capsule.readInt("depth", 0);
        mipMapSizes = capsule.readIntArray("mipMapSizes", null);
        multiSamples = capsule.readInt("multiSamples", 1);
        data = (ArrayList<ByteBuffer>) capsule.readByteBufferArrayList("data", null);
    }

}
