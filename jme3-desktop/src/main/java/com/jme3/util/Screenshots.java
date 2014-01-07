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
package com.jme3.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Screenshots {
    
    public static void convertScreenShot2(IntBuffer bgraBuf, BufferedImage out){
        WritableRaster wr = out.getRaster();
        DataBufferInt db = (DataBufferInt) wr.getDataBuffer();
        
        int[] cpuArray = db.getData();
        
        bgraBuf.clear();
        bgraBuf.get(cpuArray);
        
//        int width  = wr.getWidth();
//        int height = wr.getHeight();
//
//        // flip the components the way AWT likes them
//        for (int y = 0; y < height / 2; y++){
//            for (int x = 0; x < width; x++){
//                int inPtr  = (y * width + x);
//                int outPtr = ((height-y-1) * width + x);
//                int pixel = cpuArray[inPtr];
//                cpuArray[inPtr] = cpuArray[outPtr];
//                cpuArray[outPtr] = pixel;
//            }
//        }
    }
    
    /**
     * Flips the image along the Y axis and converts BGRA to ABGR
     * 
     * @param bgraBuf
     * @param out 
     */
    public static void convertScreenShot(ByteBuffer bgraBuf, BufferedImage out){
        WritableRaster wr = out.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();

        byte[] cpuArray = db.getData();

        // copy native memory to java memory
        bgraBuf.clear();
        bgraBuf.get(cpuArray);
        bgraBuf.clear();

        int width  = wr.getWidth();
        int height = wr.getHeight();

        // flip the components the way AWT likes them
        for (int y = 0; y < height / 2; y++){
            for (int x = 0; x < width; x++){
                int inPtr  = (y * width + x) * 4;
                int outPtr = ((height-y-1) * width + x) * 4;

                byte b1 = cpuArray[inPtr+0];
                byte g1 = cpuArray[inPtr+1];
                byte r1 = cpuArray[inPtr+2];
                byte a1 = cpuArray[inPtr+3];

                byte b2 = cpuArray[outPtr+0];
                byte g2 = cpuArray[outPtr+1];
                byte r2 = cpuArray[outPtr+2];
                byte a2 = cpuArray[outPtr+3];

                cpuArray[outPtr+0] = a1;
                cpuArray[outPtr+1] = b1;
                cpuArray[outPtr+2] = g1;
                cpuArray[outPtr+3] = r1;

                cpuArray[inPtr+0] = a2;
                cpuArray[inPtr+1] = b2;
                cpuArray[inPtr+2] = g2;
                cpuArray[inPtr+3] = r2;
            }
        }
    }
}
