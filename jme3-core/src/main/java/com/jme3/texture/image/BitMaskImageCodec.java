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

import java.nio.ByteBuffer;

class BitMaskImageCodec extends ImageCodec {
    
    // Shifts
    final int as, rs, gs, bs;
    boolean be = false;
    
    public BitMaskImageCodec(int bpp, int flags, int ac, int rc, int gc, int bc, int as, int rs, int gs, int bs) {
        super(bpp, flags,
                (int) (((long) 1 << ac) - 1),
                (int) (((long) 1 << rc) - 1),
                (int) (((long) 1 << gc) - 1),
                (int) (((long) 1 << bc) - 1));

        if (bpp > 4) {
            throw new UnsupportedOperationException("Use ByteAlignedImageCodec for codecs with pixel sizes larger than 4 bytes");
        }
        
        this.as = as;
        this.rs = rs;
        this.gs = gs;
        this.bs = bs;
    }
    
    private static int readPixelRaw(ByteBuffer buf, int idx, int bpp) {
        //idx += bpp;
        //int original = buf.get(--idx) & 0xff;
        //while ((--bpp) > 0) {
        //    original = (original << 8) | (buf.get(--idx) & 0xff);
        //}
        //return original;
        //return buf.getInt(idx) & (0xFFFFFFFF >>> (32 - bpp));
        int pixel = 0;
        buf.position(idx);
        for (int i = 0; i < bpp; i++) {
            pixel = pixel | (buf.get() & 0xff) << (i * 8);
        }
        return pixel;
    }
    
    private void writePixelRaw(ByteBuffer buf, int idx, int pixel, int bpp){
//        buf.position(idx);
//        if (!be){
        // This works:
//            while ((--bpp) >= 0){
//                byte bt = (byte) ((pixel >> (bpp * 8)) & 0xff);
//                buf.put(idx + bpp, bt);
//            }
        // ==
//        } else {
//            for (int i = bpp - 1; i >= 0; i--) {
//                byte bt = (byte) ((pixel >> (i * 8)) & 0xff);
//                buf.put(idx + i, bt);
//            }
//        }
        
        buf.position(idx);
        for (int i = 0; i < bpp; i++) {
            buf.put( (byte)((pixel >> (8 * i)) & 0xff) );
        }
    }

    @Override
    public void readComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        int inputPixel = readPixelRaw(buf, (x + y * width) * bpp + offset, bpp);
        components[0] = (inputPixel >> as) & maxAlpha;
        components[1] = (inputPixel >> rs) & maxRed;
        components[2] = (inputPixel >> gs) & maxGreen;
        components[3] = (inputPixel >> bs) & maxBlue;
    }

    public void writeComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        // Shift components then mask them
        // Map all components into a single bitspace
        int outputPixel = ((components[0] & maxAlpha) << as)
                        | ((components[1] & maxRed) << rs)
                        | ((components[2] & maxGreen) << gs)
                        | ((components[3] & maxBlue) << bs);
        
        // Find index in image where to write pixel.
        // Write the resultant bitspace into the pixel.
        writePixelRaw(buf, (x + y * width) * bpp + offset, outputPixel, bpp);
    }
}
