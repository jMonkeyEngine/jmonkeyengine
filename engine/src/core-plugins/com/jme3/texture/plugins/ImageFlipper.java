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

package com.jme3.texture.plugins;

import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 * ImageFlipper is a utility class used to flip images across the Y axis.
 * Due to the standard of where the image origin is between OpenGL and
 * other software, this class is required.
 * 
 * @author Kirill Vainer
 */
public class ImageFlipper {

    public static void flipImage(Image img, int index){
        if (img.getFormat().isCompressed())
            throw new UnsupportedOperationException("Flipping compressed " +
                                                    "images is unsupported.");

        int w = img.getWidth();
        int h = img.getHeight();
        int halfH = h / 2;

        // bytes per pixel
        int bpp = img.getFormat().getBitsPerPixel() / 8;
        int scanline = w * bpp;

        ByteBuffer data = img.getData(index);
        ByteBuffer temp = BufferUtils.createByteBuffer(scanline);
        
        data.rewind();
        for (int y = 0; y < halfH; y++){
            int oppY = h - y - 1;
            // read in scanline
            data.position(y * scanline);
            data.limit(data.position() + scanline);

            temp.rewind();
            temp.put(data);

        }
    }

}
