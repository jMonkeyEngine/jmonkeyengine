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

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ImageRaster;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MipMapGenerator {

    private MipMapGenerator() {
    }
    
    public static Image scaleImage(Image inputImage, int outputWidth, int outputHeight) {
        int size = outputWidth * outputHeight * inputImage.getFormat().getBitsPerPixel() / 8;
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        Image outputImage = new Image(inputImage.getFormat(), 
                                      outputWidth, 
                                      outputHeight, 
                                      buffer, 
                                      inputImage.getColorSpace());
        
        ImageRaster input = ImageRaster.create(inputImage, 0, 0, false);
        ImageRaster output = ImageRaster.create(outputImage, 0, 0, false);
        
        float xRatio = ((float)(input.getWidth()  - 1)) / output.getWidth();
        float yRatio = ((float)(input.getHeight() - 1)) / output.getHeight();
        
        ColorRGBA outputColor = new ColorRGBA();
        ColorRGBA bottomLeft = new ColorRGBA();
        ColorRGBA bottomRight = new ColorRGBA();
        ColorRGBA topLeft = new ColorRGBA();
        ColorRGBA topRight = new ColorRGBA();
        
        for (int y = 0; y < outputHeight; y++) {
            for (int x = 0; x < outputWidth; x++) {
                float x2f = x * xRatio;
                float y2f = y * yRatio;
                
                int x2 = (int)x2f;
                int y2 = (int)y2f;
                
                float xDiff = x2f - x2;
                float yDiff = y2f - y2;
                
                input.getPixel(x2,     y2,     bottomLeft);
                input.getPixel(x2 + 1, y2,     bottomRight);
                input.getPixel(x2,     y2 + 1, topLeft);
                input.getPixel(x2 + 1, y2 + 1, topRight);
                
                bottomLeft.multLocal(  (1f - xDiff) * (1f - yDiff) );
                bottomRight.multLocal( (xDiff)      * (1f - yDiff) );
                topLeft.multLocal(     (1f - xDiff) * (yDiff) );
                topRight.multLocal(    (xDiff)      * (yDiff) );
                
                outputColor.set(bottomLeft).addLocal(bottomRight)
                           .addLocal(topLeft).addLocal(topRight);
                
                output.setPixel(x, y, outputColor);
            }
        }
        return outputImage;
    }
    
    public static Image resizeToPowerOf2(Image original){
        int potWidth = FastMath.nearestPowerOfTwo(original.getWidth());
        int potHeight = FastMath.nearestPowerOfTwo(original.getHeight());
        return scaleImage(original, potWidth, potHeight);
    }
    
    public static void generateMipMaps(Image image){
        int width = image.getWidth();
        int height = image.getHeight();

        Image current = image;
        ArrayList<ByteBuffer> output = new ArrayList<ByteBuffer>();
        int totalSize = 0;
        
        while (height >= 1 || width >= 1){
            output.add(current.getData(0));
            totalSize += current.getData(0).capacity();

            if (height == 1 || width == 1) {
                break;
            }

            height /= 2;
            width  /= 2;

            current = scaleImage(current, width, height);
        }

        ByteBuffer combinedData = BufferUtils.createByteBuffer(totalSize);
        int[] mipSizes = new int[output.size()];
        for (int i = 0; i < output.size(); i++){
            ByteBuffer data = output.get(i);
            data.clear();
            combinedData.put(data);
            mipSizes[i] = data.capacity();
        }
        combinedData.flip();

        // insert mip data into image
        image.setData(0, combinedData);
        image.setMipMapSizes(mipSizes);
    }
}
