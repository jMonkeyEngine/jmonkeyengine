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

package jme3tools.converters;

/**
 *
 * @author Kirill
 */
public class RGB565 {

    public static short ARGB8_to_RGB565(int argb){
        int a = (argb & 0xFF000000) >> 24;
        int r = (argb & 0x00FF0000) >> 16;
        int g = (argb & 0x0000FF00) >> 8;
        int b = (argb & 0x000000FF);

        r  = r >> 3;
        g  = g >> 2;
        b  = b >> 3;

        return (short) (b | (g << 5) | (r << (5 + 6)));
    }

    public static int RGB565_to_ARGB8(short rgb565){
        int a = 0xff;
        int r = (rgb565 & 0xf800) >> 11;
        int g = (rgb565 & 0x07e0) >> 5;
        int b = (rgb565 & 0x001f);

        r  = r << 3;
        g  = g << 2;
        b  = b << 3;

        return (a << 24) | (r << 16) | (g << 8) | (b);
    }

    

}
