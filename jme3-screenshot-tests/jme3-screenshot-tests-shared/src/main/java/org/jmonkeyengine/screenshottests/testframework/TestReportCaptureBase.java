/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This creates the Extent report and manages the test lifecycle
 *
 * @author Richard Tingle (aka richtea)
 */
public abstract class TestReportCaptureBase {

    public static TestReportCaptureBase INSTANCE;


    public abstract void markFailInReport(String message);

    public abstract void warning(String logString);

    public final void attachImage(String title, String fileName, Image image){
        // image must be RBGA8 for writing
        attachImageInner(title, fileName, convertToRGBA8(image));
    }

    public abstract void attachImageInner(String title, String fileName, Image image);

    public static Image convertToRGBA8(Image image) {
        if (image.getFormat() == Image.Format.RGBA8
                && image.getColorSpace() == ColorSpace.sRGB
                && image.getData(0).order() == ByteOrder.LITTLE_ENDIAN
        ){
            image.getData(0).rewind();
            return image;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*image.getWidth() * image.getHeight());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        Image rgbaImage = new Image(Image.Format.RGBA8, image.getWidth(), image.getHeight(), byteBuffer, null, ColorSpace.sRGB);
        ImageRaster source = ImageRaster.create(image);
        ImageRaster target = ImageRaster.create(rgbaImage);

        ColorRGBA temp = new ColorRGBA();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                ColorRGBA color = source.getPixel(x, y, temp);
                color.a = 1.0f;
                target.setPixel(x, y, color);
            }
        }

        rgbaImage.getData(0).rewind();
        return rgbaImage;
    }
}
