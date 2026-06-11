/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.cursors.plugins;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;

/**
 * Convert any image like object to a {@link JmeCursor}.
 */
public class CursorConverter {
     /**
     * Convert a {@link Texture2D} to a {@link JmeCursor}.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}, the start point is 0, 0 being
     * lower left.
     * 
     * The cursor's hot spot will be the top left of the texture's image.
     * 
     * @param cursorTexture the texture to convert. No modifications will be applied.
     * @return the {@link JmeCursor} object that contains the data for a cursor.
     */
    public static JmeCursor fromTexture(Texture2D cursorTexture) {
        return fromTexture(cursorTexture, 0, cursorTexture.getImage().getHeight());
    }

    /**
     * Convert a {@link Texture2D} to a {@link JmeCursor}.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}, the start point is 0, 0 being
     * lower left.
     * 
     * @param cursorTexture the texture to convert. No modifications will be applied.
     * @param xHotspot same specification as {@link JmeCursor#setxHotSpot(int)}: the cursor's X axis
     *                  coordinate for its hotspot. Note that the coordinate system is the same as
     *                  OpenGL. 0, 0 being lower left. Must be between 0 and image width.
     * @param yHotspot same specification as {@link JmeCursor#setyHotSpot(int)}: the cursor's Y axis
     *                  coordinate for its hotspot. Note that the coordinate system is the same as
     *                  OpenGL. 0, 0 being lower left. Must be between 0 and image height.
     * @return the {@link JmeCursor} object that contains the data for a cursor.
     */
    public static JmeCursor fromTexture(Texture2D cursorTexture, int xHotspot, int yHotspot) {
        Image image = cursorTexture.getImage();

        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        if (!hotspotInsideImageLimits(xHotspot, yHotspot, imageWidth, imageHeight)) {
            throw new IllegalArgumentException(
                "Cursor's hot spot must be inside image limits (width and height)"
            );
        }

        IntBuffer adaptedImageData = getDataAsIntBuffer(image);

        JmeCursor jmeCursor = new JmeCursor();
        jmeCursor.setWidth(imageWidth);
        jmeCursor.setHeight(imageHeight);
        jmeCursor.setxHotSpot(xHotspot);
        jmeCursor.setyHotSpot(yHotspot);
        jmeCursor.setNumImages(1);
        jmeCursor.setImagesDelay(null);
        jmeCursor.setImagesData(adaptedImageData);
        return jmeCursor;
    }

    /**
     * Convert a {@link Texture2D} array to a {@link JmeCursor} object that will represent an animated cursor,
     * interpreting each {@link Texture2D} object as a frame of the animated cursor.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}. The start point is 0, 0 being
     * lower left.
     * 
     * The cursor's hot spot will be the top left of the texture's image.
     * 
     * @param cursorFrames the frames that will make up the cursor animation.
     *                     No modifications will be applied.
     * @param frameDelay the time delay that will take for a cursor to change from one frame to another.
     * @return a {@link JmeCursor} object that contains the data for an animated cursor.
     */
    public static JmeCursor fromTextureFrames(Texture2D[] cursorFrames, int frameDelay) {
        int yHotspot = 0;
        if (cursorFrames.length > 0) {
            yHotspot = cursorFrames[0].getImage().getHeight();
        }
        return fromTextureFrames(cursorFrames, frameDelay, 0, yHotspot);
    }

    /**
     * Convert a {@link Texture2D} array to a {@link JmeCursor} object that will represent an animated cursor,
     * interpreting each {@link Texture2D} object as a frame of the animated cursor.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}. The start point is 0, 0 being
     * lower left.
     * 
     * The cursor's hot spot will be the top left of the texture's image.
     * 
     * @param cursorFrames the frames that will make up the cursor animation.
     *                     No modifications will be applied.
     * @param frameDelay the time delay that will take for a cursor to change from one frame to another.
     * @return a {@link JmeCursor} object that contains the data for an animated cursor.
     */
    public static JmeCursor  fromTextureFrames(Texture2D[] cursorFrames, int[] frameDelays) {
        int yHotspot = 0;
        if (cursorFrames.length > 0) {
            yHotspot = cursorFrames[0].getImage().getHeight();
        }
        return fromTextureFrames(cursorFrames, frameDelays, 0, yHotspot);
    }

    /**
     * Convert a {@link Texture2D} array to a {@link JmeCursor} object that will represent an animated cursor,
     * interpreting each {@link Texture2D} object as a frame of the animated cursor.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}. The start point is 0, 0 being 
     * lower left.
     * 
     * @param cursorFrames the frames that will make up the cursor animation.
     *                     No modifications will be applied.
     * @param frameDelay the time delay that will take for a cursor to change from one frame to another.
     * @param xHotspot same specification as {@link JmeCursor#setxHotSpot(int)}: the cursor's X axis
     *                 coordinate for its hotspot. Note that the coordinate system is the same as
     *                 OpenGL. 0, 0 being lower left. Must be between 0 and image width.
     * @param yHotspot same specification as {@link JmeCursor#setyHotSpot(int)}: the cursor's Y axis
     *                 coordinate for its hotspot. Note that the coordinate system is the same as
     *                 OpenGL. 0, 0 being lower left. Must be between 0 and image height.
     * @return a {@link JmeCursor} object that contains the data for an animated cursor.
     */
    public static JmeCursor fromTextureFrames(
            Texture2D[] cursorFrames, 
            int frameDelay, 
            int xHotspot, 
            int yHotspot) {
        int[] frameRates = new int[cursorFrames.length];
        Arrays.fill(frameRates, frameDelay);
        return fromTextureFrames(cursorFrames, frameRates, xHotspot, yHotspot);
    }

    /**
     * Convert a {@link Texture2D} array to a {@link JmeCursor} object that will represent an animated cursor,
     * interpreting each {@link Texture2D} object as a frame of the animated cursor.
     * 
     * The coordinate system used is the same specified in {@link JmeCursor}. The start point is 0, 0 being 
     * lower left.
     * 
     * @param cursorFrames the frames that will make up the cursor animation.
     *                     No modifications will be applied.
     * @param frameDelays the time delay that will take each frame to change to the next frame. Because of it,
     *                    it must contains as many delays as frames (lengths of cursorFrames and frameDelays 
     *                    arrays must be equal).
     * @param xHotspot tame specification as {@link JmeCursor#setxHotSpot(int)}: the cursor's X axis
     *                 coordinate for its hotspot. Note that the coordinate system is the same as
     *                 OpenGL. 0, 0 being lower left. Must be between 0 and image width.
     * @param yHotspot same specification as {@link JmeCursor#setyHotSpot(int)}: the cursor's Y axis
     *                 coordinate for its hotspot. Note that the coordinate system is the same as
     *                 OpenGL. 0, 0 being lower left. Must be between 0 and image height.
     * @return a {@link JmeCursor} object that contains the data for an animated cursor.
     */
    public static JmeCursor fromTextureFrames(
            Texture2D[] cursorFrames, 
            int[] frameDelays, 
            int xHotspot, 
            int yHotspot) {
        if (frameDelays.length != cursorFrames.length) {
            throw new IllegalArgumentException(
                    "The lengths of cursorFrames and frameDelays arrays must be equal"
            );
        }

        List<Image> imageFrames = Arrays.stream(cursorFrames)
                //Avoid working and accidentally modifying original values
                .map((frame) -> frame.getImage())
                .collect(Collectors.toList());

        List<Integer> imageFrameHeights = imageFrames
                .stream()
                .map((image) -> image.getHeight())
                .distinct()
                .collect(Collectors.toList());
        
        List<Integer> imageFrameWidths = imageFrames
                .stream()
                .map((image) -> image.getWidth())
                .distinct()
                .collect(Collectors.toList());
        
        if (imageFrameHeights.size() > 1 || imageFrameWidths.size() > 1) {
            throw new IllegalArgumentException("Some images from the Texture2D objects have different sizes");
        }

        int imageHeight = imageFrameHeights.get(0);
        int imageWidth = imageFrameWidths.get(0);

        if (!hotspotInsideImageLimits(xHotspot, yHotspot, imageWidth, imageHeight)) {
            throw new IllegalArgumentException(
                "Cursor's hot spot must be inside image limits (width and height)"
            );
        }

        IntBuffer imagesData = BufferUtils.createIntBuffer(imageHeight * imageWidth * cursorFrames.length);

        List<IntBuffer> framesData = imageFrames
                .stream()
                .map((image) -> getDataAsIntBuffer(image))
                .collect(Collectors.toList());

        for (IntBuffer frameData : framesData) {
            imagesData.put(frameData);
        }
        imagesData.flip();

        JmeCursor jmeCursor = new JmeCursor();
        jmeCursor.setWidth(imageWidth);
        jmeCursor.setHeight(imageHeight);
        jmeCursor.setxHotSpot(xHotspot);
        jmeCursor.setyHotSpot(yHotspot);
        jmeCursor.setNumImages(cursorFrames.length);
        jmeCursor.setImagesDelay(BufferUtils.createIntBuffer(frameDelays));
        jmeCursor.setImagesData(imagesData);
        return jmeCursor;
    }

    private static IntBuffer getDataAsIntBuffer(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        ImageRaster raster = ImageRaster.create(image);
        
        IntBuffer data = BufferUtils.createIntBuffer(width * height);
        
        //ARGB color system is needed to show cursors correctly.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ColorRGBA color = raster.getPixel(x, y);
                
                int a = (int) (color.a * 255) & 0xFF;
                int r = (int) (color.r * 255) & 0xFF;
                int g = (int) (color.g * 255) & 0xFF;
                int b = (int) (color.b * 255) & 0xFF;

                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                
                data.put(argb);
            }
        }
        
        data.flip();
        return data;
    }

    private static boolean hotspotInsideImageLimits(
            int xHotspot, 
            int yHotspot, 
            int imageWidth, 
            int imageHeight) {
        return xHotspot >= 0 && xHotspot <= imageWidth && yHotspot >= 0 && yHotspot <= imageHeight;
    }
}
