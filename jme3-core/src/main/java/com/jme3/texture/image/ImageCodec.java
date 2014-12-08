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

import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;
import java.util.EnumMap;

abstract class ImageCodec {
    
    public static final int FLAG_F16 = 1, FLAG_F32 = 2, FLAG_GRAY = 4; //, FLAG_ALPHAONLY = 8, FLAG_SHAREDEXP = 16;
    private static final EnumMap<Image.Format, ImageCodec> params = new EnumMap<Image.Format, ImageCodec>(Image.Format.class);
    
    protected final int bpp, type, maxAlpha, maxRed, maxGreen, maxBlue;
    protected final boolean isGray;

    public ImageCodec(int bpp, int flags, int maxAlpha, int maxRed, int maxGreen, int maxBlue) {
        this.bpp = bpp;
        this.isGray = (flags & FLAG_GRAY) != 0;
        this.type = flags & ~FLAG_GRAY;
        this.maxAlpha = maxAlpha;
        this.maxRed = maxRed;
        this.maxGreen = maxGreen;
        this.maxBlue = maxBlue;
    }

    static {       
        // == ALPHA ==
//        params.put(Format.Alpha8,   new BitMaskImageCodec(1, 0, 8, 0, 0, 0,
//                                                                0, 0, 0, 0));
        
        params.put(Format.Alpha8,   new ByteOffsetImageCodec(1, 0, 0, -1, -1, -1));
        
        // == LUMINANCE ==
//        params.put(Format.Luminance8, new BitMaskImageCodec(1, FLAG_GRAY, 0, 8, 0, 0,
//                                                                          0, 0, 0, 0));
        
        params.put(Format.Luminance8, new ByteOffsetImageCodec(1, FLAG_GRAY, -1, 0, -1, -1));
        
        params.put(Format.Luminance16F, new BitMaskImageCodec(2, FLAG_GRAY | FLAG_F16, 0, 16, 0, 0,
                                                                                        0, 0, 0, 0));
        params.put(Format.Luminance32F, new BitMaskImageCodec(4, FLAG_GRAY | FLAG_F32, 0, 32, 0, 0,
                                                                                        0, 0, 0, 0));
        
        // == INTENSITY ==
        // ??
        
        // == LUMINANCA ALPHA ==
//        params.put(Format.Luminance8Alpha8, new BitMaskImageCodec(2, FLAG_GRAY, 
//                                                                  8, 8, 0, 0,
//                                                                  8, 0, 0, 0));
        
        params.put(Format.Luminance8Alpha8, new ByteOffsetImageCodec(2, FLAG_GRAY, 1, 0, -1, -1));
        
        params.put(Format.Luminance16FAlpha16F, new BitMaskImageCodec(4, FLAG_GRAY | FLAG_F16, 
                                                                   16, 16, 0, 0,
                                                                   16, 0, 0, 0));
        
        // == RGB ==
//        params.put(Format.BGR8,     new BitMaskImageCodec(3, 0, 
//                                                          0, 8,  8,  8,
//                                                          0, 16, 8,  0));
//        
        params.put(Format.BGR8,     new ByteOffsetImageCodec(3, 0, -1, 2, 1, 0));
        
        params.put(Format.RGB565,       new BitMaskImageCodec(2, 0,
                                                            0, 5,  6, 5,
                                                            0, 11, 5, 0));
//        
//        params.put(Format.RGB8,         new BitMaskImageCodec(3, 0,
//                                                            0, 8, 8, 8,
//                                                            0, 0, 8, 16));
        
        params.put(Format.RGB8,     new ByteOffsetImageCodec(3, 0, -1, 0, 1, 2));
       
        params.put(Format.RGB32F,        new ByteAlignedImageCodec(12, FLAG_F32,
                                                                   0,  4, 4, 4,
                                                                   0,  0, 4, 8));
        
        ByteAlignedImageCodec rgb16f = new ByteAlignedImageCodec(6, FLAG_F16,
                                                            0, 2, 2, 2,
                                                            0, 0, 2, 4); 
        params.put(Format.RGB16F, rgb16f);
        params.put(Format.RGB16F_to_RGB111110F, rgb16f);
        params.put(Format.RGB16F_to_RGB9E5, rgb16f);
        
        // == RGBA ==
//        params.put(Format.ABGR8,    new BitMaskImageCodec(4, 0,
//                                                          0, 8, 8, 8,
//                                                          0, 24, 16, 8));
        
        params.put(Format.ABGR8, new ByteOffsetImageCodec(4, 0, 0, 3, 2, 1));
        
        params.put(Format.ARGB8, new ByteOffsetImageCodec(4, 0, 0, 1, 2, 3));
        
        params.put(Format.BGRA8, new ByteOffsetImageCodec(4, 0, 3, 2, 1, 0));
       
        params.put(Format.RGB5A1,   new BitMaskImageCodec(2, 0, 
                                                          1, 5, 5, 5,
                                                          0, 11, 6, 1));
        ((BitMaskImageCodec)params.get(Format.RGB5A1)).be = true;
       
//        params.put(Format.RGBA8,    new ByteAlignedImageCodec(4, 0,
//                                                              0, 1, 1, 1,
//                                                              0, 0, 1, 2));
                
                //new BitMaskImageCodec(4, 0,
                                    //                      8,  8, 8, 8,
                                    //                      24,  0, 8, 16));
        
        params.put(Format.RGBA8, new ByteOffsetImageCodec(4, 0, 3, 0, 1, 2));
        
        params.put(Format.RGBA16F,        new ByteAlignedImageCodec(8, FLAG_F16,
                                                            2, 2, 2,  2,
                                                            6, 0, 2,  4));
        
        params.put(Format.RGBA32F,        new ByteAlignedImageCodec(16, FLAG_F32,
                                                            4, 4, 4, 4,
                                                            12, 0, 4, 8));
    }
    
    public abstract void readComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp);
    
    public abstract void writeComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp);
    
    /**
     * Looks up the format in the codec registry.
     * The codec will be able to decode the given format.
     * 
     * @param format The format to lookup.
     * @return The codec capable of decoding it, or null if not found.
     */
    public static ImageCodec lookup(Format format) {
        ImageCodec codec = params.get(format);
        if (codec == null) {
            throw new UnsupportedOperationException("The format " + format + " is not supported");
        }
        return codec;
    }
}
