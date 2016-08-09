/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.texture.image;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;

public class DefaultImageRaster extends ImageRaster {
    
    private final int[] components = new int[4];
    private ByteBuffer buffer;
    private final Image image;
    private final ImageCodec codec;
    private final int width;
    private final int height;
    private final int offset;
    private final byte[] temp;
    private final boolean convertToLinear;
    private int slice;
    
    private void rangeCheck(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("x and y must be inside the image dimensions:" 
                                                + x + ", " + y + " in:" + width + ", " + height);
        }
    }
    
    public DefaultImageRaster(Image image, int slice, int mipMapLevel, boolean convertToLinear) {
        int[] mipMapSizes = image.getMipMapSizes();
        int availableMips = mipMapSizes != null ? mipMapSizes.length : 1;
        
        if (mipMapLevel >= availableMips) {
            throw new IllegalStateException("Cannot create image raster for mipmap level #" + mipMapLevel + ". "
                                          + "Image only has " + availableMips + " mipmap levels.");
        }
        
        if (image.hasMipmaps()) {
            this.width  = Math.max(1, image.getWidth()  >> mipMapLevel);
            this.height = Math.max(1, image.getHeight() >> mipMapLevel);
            
            int mipOffset = 0;
            for (int i = 0; i < mipMapLevel; i++) {
                mipOffset += mipMapSizes[i];
            }
            
            this.offset = mipOffset;
        } else {
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.offset = 0;
        }
        
        this.image = image;
        this.slice = slice;
        
        // Conversion to linear only needed if image's color space is sRGB.
        this.convertToLinear = convertToLinear && image.getColorSpace() == ColorSpace.sRGB;
        
        this.buffer = image.getData(slice);
        this.codec = ImageCodec.lookup(image.getFormat());
        
        if (codec instanceof ByteAlignedImageCodec || codec instanceof ByteOffsetImageCodec) {
            this.temp = new byte[codec.bpp];
        } else {
            this.temp = null;
        }
    }
    
    public void setSlice(int slice) {
        this.slice = slice;
        this.buffer = image.getData(slice);
    }
    
    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setPixel(int x, int y, ColorRGBA color) {
        rangeCheck(x, y);
        
        if (convertToLinear) {
            // Input is linear, needs to be converted to sRGB before writing
            // into image.
            color = color.getAsSrgb();
        }
        
        // Check flags for grayscale
        if (codec.isGray) {
            float gray = color.r * 0.27f + color.g * 0.67f + color.b * 0.06f;
            color = new ColorRGBA(gray, gray, gray, color.a);
        }

        switch (codec.type) {
            case ImageCodec.FLAG_F16:
                components[0] = (int) FastMath.convertFloatToHalf(color.a);
                components[1] = (int) FastMath.convertFloatToHalf(color.r);
                components[2] = (int) FastMath.convertFloatToHalf(color.g);
                components[3] = (int) FastMath.convertFloatToHalf(color.b);
                break;
            case ImageCodec.FLAG_F32:
                components[0] = (int) Float.floatToIntBits(color.a);
                components[1] = (int) Float.floatToIntBits(color.r);
                components[2] = (int) Float.floatToIntBits(color.g);
                components[3] = (int) Float.floatToIntBits(color.b);
                break;
            case 0:
                // Convert color to bits by multiplying by size
                components[0] = Math.min( (int) (color.a * codec.maxAlpha + 0.5f), codec.maxAlpha);
                components[1] = Math.min( (int) (color.r * codec.maxRed + 0.5f), codec.maxRed);
                components[2] = Math.min( (int) (color.g * codec.maxGreen + 0.5f), codec.maxGreen);
                components[3] = Math.min( (int) (color.b * codec.maxBlue + 0.5f), codec.maxBlue);
                break;
        }     
        codec.writeComponents(getBuffer(), x, y, width, offset, components, temp);
        image.setUpdateNeeded();
    }
    
    private ByteBuffer getBuffer(){
        if(buffer == null){
            this.buffer = image.getData(slice);
        }
        return buffer;
    }
    
    @Override
    public ColorRGBA getPixel(int x, int y, ColorRGBA store) {
        rangeCheck(x, y);
        
        codec.readComponents(getBuffer(), x, y, width, offset, components, temp);
        if (store == null) {
            store = new ColorRGBA();
        }
        switch (codec.type) {
            case ImageCodec.FLAG_F16:
                store.set(FastMath.convertHalfToFloat((short)components[1]),
                          FastMath.convertHalfToFloat((short)components[2]),
                          FastMath.convertHalfToFloat((short)components[3]),
                          FastMath.convertHalfToFloat((short)components[0]));
                break;
            case ImageCodec.FLAG_F32:
                store.set(Float.intBitsToFloat((int)components[1]),
                          Float.intBitsToFloat((int)components[2]),
                          Float.intBitsToFloat((int)components[3]),
                          Float.intBitsToFloat((int)components[0]));
                break;
            case 0:
                // Convert to float and divide by bitsize to get into range 0.0 - 1.0.
                store.set((float)components[1] / codec.maxRed,
                          (float)components[2] / codec.maxGreen,
                          (float)components[3] / codec.maxBlue,
                          (float)components[0] / codec.maxAlpha);
                break;
        }
        if (codec.isGray) {
            store.g = store.b = store.r;
        } else {
            if (codec.maxRed == 0) {
                store.r = 1;
            }
            if (codec.maxGreen == 0) {
                store.g = 1;
            }
            if (codec.maxBlue == 0) {
                store.b = 1;
            }
            if (codec.maxAlpha == 0) {
                store.a = 1;
            }
        }
        
        if (convertToLinear) {
            // Input image is sRGB, need to convert to linear.
            store.setAsSrgb(store.r, store.g, store.b, store.a);
        }
        
        return store;
    }
}
