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

package com.jme3.terrain.heightmap;

import java.nio.ByteBuffer;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import java.nio.ShortBuffer;

/**
 * <code>ImageBasedHeightMap</code> is a height map created from the grayscale
 * conversion of an image. The image used currently must have an equal height
 * and width, although future work could scale an incoming image to a specific
 * height and width.
 * 
 * @author Mike Kienenberger
 * @version $id$
 */
public class ImageBasedHeightMap extends AbstractHeightMap implements ImageHeightmap {
    
    
    protected Image colorImage;

    
    public void setImage(Image image) {
        this.colorImage = image;
    }
    
    /**
     * Creates a HeightMap from an Image. The image will be converted to
     * grayscale, and the grayscale values will be used to generate the height
     * map. White is highest point while black is lowest point.
     * 
     * Currently, the Image used must be square (width == height), but future
     * work could rescale the image.
     * 
     * @param colorImage
     *            Image to map to the height map.
     */
    public ImageBasedHeightMap(Image colorImage) {
        this.colorImage = colorImage;
    }
    
    public ImageBasedHeightMap(Image colorImage, float heightScale) {
    	this.colorImage = colorImage;
        this.heightScale = heightScale;
    }

    /**
     * Loads the image data from top left to bottom right
     */
    public boolean load() {
        return load(false, false);
    }

    /**
     * Get the grayscale value, or override in your own sub-classes
     */
    protected float calculateHeight(float red, float green, float blue) {
        return (float) (0.299 * red + 0.587 * green + 0.114 * blue);
    }
    
    public boolean load(boolean flipX, boolean flipY) {

        int imageWidth = colorImage.getWidth();
        int imageHeight = colorImage.getHeight();

        if (imageWidth != imageHeight)
                throw new RuntimeException("imageWidth: " + imageWidth
                        + " != imageHeight: " + imageHeight);

        size = imageWidth;

        ByteBuffer buf = colorImage.getData(0);

        heightData = new float[(imageWidth * imageHeight)];

        ColorRGBA colorStore = new ColorRGBA();
        
        int index = 0;
        if (flipY) {
            for (int h = 0; h < imageHeight; ++h) {
                if (flipX) {
                    for (int w = imageWidth - 1; w >= 0; --w) {
                        int baseIndex = (h * imageWidth)+ w;
                        heightData[index++] = getHeightAtPostion(buf, colorImage, baseIndex, colorStore)*heightScale;
                    }
                } else {
                    for (int w = 0; w < imageWidth; ++w) {
                        int baseIndex = (h * imageWidth)+ w;
                        heightData[index++] = getHeightAtPostion(buf, colorImage, baseIndex, colorStore)*heightScale;
                    }
                }
            }
        } else {
            for (int h = imageHeight - 1; h >= 0; --h) {
                if (flipX) {
                    for (int w = imageWidth - 1; w >= 0; --w) {
                        int baseIndex = (h * imageWidth)+ w;
                        heightData[index++] = getHeightAtPostion(buf, colorImage, baseIndex, colorStore)*heightScale;
                    }
                } else {
                    for (int w = 0; w < imageWidth; ++w) {
                        int baseIndex = (h * imageWidth)+ w;
                        heightData[index++] = getHeightAtPostion(buf, colorImage, baseIndex, colorStore)*heightScale;
                    }
                }
            }
        }

        return true;
    }
    
    protected float getHeightAtPostion(ByteBuffer buf, Image image, int position, ColorRGBA store) {
        switch (image.getFormat()){
            case RGBA8:
                buf.position( position * 4 );
                store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));
                return calculateHeight(store.r, store.g, store.b);
            case ABGR8:
                buf.position( position * 4 );
                float a = byte2float(buf.get());
                float b = byte2float(buf.get());
                float g = byte2float(buf.get());
                float r = byte2float(buf.get());
                store.set(r,g,b,a);
                return calculateHeight(store.r, store.g, store.b);
            case RGB8:
                buf.position( position * 3 );
                store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), 1);
                return calculateHeight(store.r, store.g, store.b);
            case Luminance8:
                buf.position( position );
                return byte2float(buf.get())*255*heightScale;
            case Luminance16:
                ShortBuffer sbuf = buf.asShortBuffer();
                sbuf.position( position );
                return (sbuf.get() & 0xFFFF) / 65535f * 255f * heightScale;
            default:
                throw new UnsupportedOperationException("Image format: "+image.getFormat());
        }
    }
    
    private float byte2float(byte b){
        return ((float)(b & 0xFF)) / 255f;
    }
}