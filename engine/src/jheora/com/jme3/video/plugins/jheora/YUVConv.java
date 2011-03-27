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

package com.jme3.video.plugins.jheora;

import com.fluendo.jheora.YUVBuffer;

@Deprecated
public final class YUVConv {

    private int[] pixels;

    private static final int VAL_RANGE = 256;
    private static final int SHIFT = 16;

    private static final int CR_FAC = (int) (1.402 * (1 << SHIFT));
    private static final int CB_FAC = (int) (1.772 * (1 << SHIFT));
    private static final int CR_DIFF_FAC = (int) (0.71414 * (1 << SHIFT));
    private static final int CB_DIFF_FAC = (int) (0.34414 * (1 << SHIFT));

    private static int[] r_tab = new int[VAL_RANGE * 3];
    private static int[] g_tab = new int[VAL_RANGE * 3];
    private static int[] b_tab = new int[VAL_RANGE * 3];

    static {
        setupRgbYuvAccelerators();
    }

    private static final short clamp255(int val) {
        val -= 255;
        val = -(255 + ((val >> (31)) & val));
        return (short) -((val >> 31) & val);
    }

    private static void setupRgbYuvAccelerators() {
        for (int i = 0; i < VAL_RANGE * 3; i++) {
            r_tab[i] = clamp255(i - VAL_RANGE);
            g_tab[i] = clamp255(i - VAL_RANGE) << 8;
            b_tab[i] = clamp255(i - VAL_RANGE) << 16;
        }
    }

    public YUVConv(){
    }

    public int[] getRGBData(){
        return pixels;
    }

    public void convert(YUVBuffer yuv, int xOff, int yOff, int width, int height) {
        if (pixels == null){
            pixels = new int[width*height];
        }
        // Set up starting values for YUV pointers
        int YPtr = yuv.y_offset + xOff + yOff * (yuv.y_stride);
        int YPtr2 = YPtr + yuv.y_stride;
        int UPtr = yuv.u_offset + xOff/2 + (yOff/2)*(yuv.uv_stride);
        int VPtr = yuv.v_offset + xOff/2 + (yOff/2)*(yuv.uv_stride);
        int RGBPtr = 0;
        int RGBPtr2 = width;
        int width2 = width / 2;
        int height2 = height / 2;

        // Set the line step for the Y and UV planes and YPtr2
        int YStep = yuv.y_stride * 2 - (width2) * 2;
        int UVStep = yuv.uv_stride - (width2);
        int RGBStep = width;

        for (int i = 0; i < height2; i++) {
            for (int j = 0; j < width2; j++) {
                // groups of four pixels
                int UFactor = yuv.data[UPtr++] - 128;
                int VFactor = yuv.data[VPtr++] - 128;
                int GFactor = UFactor * CR_DIFF_FAC + VFactor * CB_DIFF_FAC - (VAL_RANGE<<SHIFT);
                
                UFactor = UFactor * CR_FAC + (VAL_RANGE<<SHIFT);
                VFactor = VFactor * CB_FAC + (VAL_RANGE<<SHIFT);

                int YVal = yuv.data[YPtr] << SHIFT;
                pixels[RGBPtr] = r_tab[(YVal + VFactor)>>SHIFT] |
                                 b_tab[(YVal + UFactor)>>SHIFT] |
                                 g_tab[(YVal - GFactor)>>SHIFT];

                YVal = yuv.data[YPtr+1] << SHIFT;
                pixels[RGBPtr+1] = r_tab[(YVal + VFactor)>>SHIFT] |
                                   b_tab[(YVal + UFactor)>>SHIFT] |
                                   g_tab[(YVal - GFactor)>>SHIFT];

                YVal = yuv.data[YPtr2] << SHIFT;
                pixels[RGBPtr2] = r_tab[(YVal + VFactor)>>SHIFT] |
                                  b_tab[(YVal + UFactor)>>SHIFT] |
                                  g_tab[(YVal - GFactor)>>SHIFT];

                YVal = yuv.data[YPtr2+1] << SHIFT;
                pixels[RGBPtr2+1] = r_tab[(YVal + VFactor)>>SHIFT] |
                                    b_tab[(YVal + UFactor)>>SHIFT] |
                                    g_tab[(YVal - GFactor)>>SHIFT];

                YPtr += 2;
                YPtr2 += 2;
                RGBPtr += 2;
                RGBPtr2 += 2;
            }

            // Increment the various pointers
            YPtr += YStep;
            YPtr2 += YStep;
            UPtr += UVStep;
            VPtr += UVStep;
            RGBPtr += RGBStep;
            RGBPtr2 += RGBStep;
        }
    }
}
