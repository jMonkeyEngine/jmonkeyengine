/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;

/**
 * Loads textures using Android's Bitmap class, but does not have the 
 * RGBA8 alpha bug.
 *
 * See below link for supported image formats:
 * https://developer.android.com/guide/topics/media/media-formats#image-formats
 * 
 * @author Kirill Vainer
 */
public class AndroidBufferImageLoader implements AssetLoader {
    
    private final byte[] tempData = new byte[16 * 1024];
    
    private static void convertARGBtoABGR(int[] src, int srcOff, int[] dst, int dstOff, int length) {
        for (int i = 0; i < length; i++) {
            int argb = src[srcOff + i];
            int a = (argb & 0xFF000000);
            int b = (argb & 0x000000FF) << 16;
            int g = (argb & 0x0000FF00);
            int r = (argb & 0x00FF0000) >> 16;
            int abgr = a | b | g | r;
            dst[dstOff + i] = abgr;
        }
    }
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        Bitmap bitmap;
        Image.Format format;
        int bpp;
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inTempStorage = tempData;
        options.inScaled = false;
        options.inDither = false;
        options.inInputShareable = true;
        options.inPurgeable = true;
        options.inSampleSize = 1;
        // Do not premultiply alpha channel as it is not intended
        // to be directly drawn by the android view system.
        options.inPremultiplied = false;

        // TODO: It is more GC friendly to reuse the Bitmap class instead of recycling
        //  it on every image load. Android has introduced inBitmap option For this purpose.
        //  However, there are certain restrictions with how inBitmap can be used.
        //  See https://developer.android.com/topic/performance/graphics/manage-memory#inBitmap.

        try (final BufferedInputStream bin = new BufferedInputStream(assetInfo.openStream())) {
            bitmap = BitmapFactory.decodeStream(bin, null, options);
            if (bitmap == null) {
                throw new IOException("Failed to load image: " + assetInfo.getKey().getName());
            }
        }

        switch (bitmap.getConfig()) {
            case ALPHA_8:
                format = Image.Format.Alpha8;
                bpp = 1;
                break;
            case ARGB_8888:
                format = Image.Format.RGBA8;
                bpp = 4;
                break;
            case RGB_565:
                format = Image.Format.RGB565;
                bpp = 2;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized Android bitmap format: " + bitmap.getConfig());
        }

        TextureKey texKey = (TextureKey) assetInfo.getKey();
        
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        ByteBuffer data = BufferUtils.createByteBuffer(bitmap.getWidth() * bitmap.getHeight() * bpp);
        
        if (format == Image.Format.RGBA8) {
            int[] pixelData = new int[width * height];
            bitmap.getPixels(pixelData, 0,  width, 0, 0,          width,  height);

            if (texKey.isFlipY()) {
                int[] sln = new int[width];
                int y2;
                for (int y1 = 0; y1 < height / 2; y1++){
                    y2 = height - y1 - 1;
                    convertARGBtoABGR(pixelData, y1 * width, sln, 0,         width);
                    convertARGBtoABGR(pixelData, y2 * width, pixelData, y1 * width, width);
                    System.arraycopy (sln,       0,          pixelData, y2 * width, width);
                }
            } else {
                convertARGBtoABGR(pixelData, 0, pixelData, 0, pixelData.length);
            }
            
            data.asIntBuffer().put(pixelData);
        } else {
            if (texKey.isFlipY()) {
                // Flip the image, then delete the old one.
                Matrix flipMat = new Matrix();
                flipMat.preScale(1.0f, -1.0f);
                Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flipMat, false);
                bitmap.recycle();
                bitmap = newBitmap;
                
                if (bitmap == null) {
                    throw new IOException("Failed to flip image: " + texKey);
                }
            }
            
            bitmap.copyPixelsToBuffer(data);
        }
        
        data.flip();
        
        bitmap.recycle();
        
        Image image = new Image(format, width, height, data, ColorSpace.sRGB);
        
        return image;
    }
}
