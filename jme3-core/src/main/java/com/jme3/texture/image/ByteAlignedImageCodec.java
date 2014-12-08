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

class ByteAlignedImageCodec extends ImageCodec {
    
    private final int ap, az, rp, rz, gp, gz, bp, bz;
    boolean be;
    
    public ByteAlignedImageCodec(int bpp, int flags, int az, int rz, int gz, int bz, int ap, int rp, int gp, int bp) {
        // Cast to long to compute max vals, since some components could be as high as 32 bits.
        super(bpp, flags, 
                (int)(((long)1 << (az << 3)) - 1), 
                (int)(((long)1 << (rz << 3)) - 1), 
                (int)(((long)1 << (gz << 3)) - 1), 
                (int)(((long)1 << (bz << 3)) - 1));

        this.ap = ap;
        this.az = az;
        this.rp = rp;
        this.rz = rz;

        this.gp = gp;
        this.gz = gz;
        this.bp = bp;
        this.bz = bz;
    }
    
    private static void readPixelRaw(ByteBuffer buf, int idx, int bpp, byte[] result) {
        buf.position(idx);
        buf.get(result, 0, bpp);
    }
    
    private static void writePixelRaw(ByteBuffer buf, int idx, byte[] pixel, int bpp) {
//        try {
        buf.position(idx);
        buf.put(pixel, 0, bpp);
//        } catch (IndexOutOfBoundsException ex) {
//            System.out.println("!");
//        }
    }
    
    private static int readComponent(byte[] encoded, int position, int size) {
//        int component = encoded[position] & 0xff;
//        while ((--size) > 0){
//            component = (component << 8) | (encoded[++position] & 0xff);
//        }
//        return component;
        try {
            int component = 0;
            for (int i = size - 1; i >= 0; i--) {
                component = (component << 8) | (encoded[position + i] & 0xff);
            }
            return component;
//        position += size - 1;
//        
//        while ((--size) >= 0) {
//            component = (component << 8) | (encoded[position--] & 0xff);
//        }
//        return component;
        } catch (ArrayIndexOutOfBoundsException ex){
            ex.printStackTrace();
            return 0;
        }
    }
    
    private void writeComponent(int component, int position, int size, byte[] result) {
//        if (!be) {
//            while ((--size) >= 0){
//                byte bt = (byte) ((component >> (size * 8)) & 0xff);
//                result[position++] = bt;
//            }
//        } else {
            for (int i = 0; i < size; i++) {
                byte bt = (byte) ((component >> (i * 8)) & 0xff);
                result[position++] = bt;
            }
//        }
    }
    
    public void readComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        readPixelRaw(buf, (x + y * width + offset) * bpp + offset, bpp, tmp);
        components[0] = readComponent(tmp, ap, az);
        components[1] = readComponent(tmp, rp, rz);
        components[2] = readComponent(tmp, gp, gz);
        components[3] = readComponent(tmp, bp, bz);
    }
    
    public void writeComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        writeComponent(components[0], ap, az, tmp);
        writeComponent(components[1], rp, rz, tmp);
        writeComponent(components[2], gp, gz, tmp);
        writeComponent(components[3], bp, bz, tmp);
        writePixelRaw(buf, (x + y * width) * bpp + offset, tmp, bpp);
    }
}
