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

public class ByteOffsetImageCodec extends ImageCodec {

    private final int redPos, greenPos, bluePos, alphaPos;
    
    public ByteOffsetImageCodec(int bpp, int flags,  int alphaPos, int redPos, int greenPos, int bluePos) {
        super(bpp, flags, alphaPos != -1 ? 255 : 0,
                          redPos != -1 ? 255 : 0,
                          greenPos != -1 ? 255 : 0,
                          bluePos != -1 ? 255 : 0);
        this.alphaPos = alphaPos;
        this.redPos = redPos;
        this.greenPos = greenPos;
        this.bluePos = bluePos;
    }
    
    @Override
    public void readComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        int i = (y * width + x) * bpp + offset;
        buf.position(i);
        buf.get(tmp, 0, bpp);
        if (alphaPos != -1) {
            components[0] = tmp[alphaPos] & 0xff;
        }
        if (redPos != -1) {
            components[1] = tmp[redPos] & 0xff;
        }
        if (greenPos != -1) {
            components[2] = tmp[greenPos] & 0xff;
        }
        if (bluePos != -1) {
            components[3] = tmp[bluePos] & 0xff;
        }
    }

    @Override
    public void writeComponents(ByteBuffer buf, int x, int y, int width, int offset, int[] components, byte[] tmp) {
        int i = (y * width + x) * bpp + offset;
        if (alphaPos != -1) {
            tmp[alphaPos] = (byte) components[0];
        }
        if (redPos != -1) {
            tmp[redPos] = (byte) components[1];
        }
        if (greenPos != -1) {
            tmp[greenPos] = (byte) components[2];
        }
        if (bluePos != -1) {
            tmp[bluePos] = (byte) components[3];
        }
        buf.position(i);
        buf.put(tmp, 0, bpp);
    }
    
}
