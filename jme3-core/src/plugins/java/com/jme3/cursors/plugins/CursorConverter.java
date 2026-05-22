package com.jme3.cursors.plugins;

import java.nio.IntBuffer;

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
     * Doesn't support cursor animations.
     * The coordinate system used is the same specified in {@link JmeCursor}. The start
     * point is 0, 0 being lower left.
     * 
     * @param cursorImage The texture to convert. No modification will be done to the object.
     * 
     * @return The {@link JmeCursor} using a deep copy of {@link Texture2D.getImage}.
     */
    public static JmeCursor fromTexture(Texture2D cursorImage) {
        Image image = cursorImage.getImage().clone();

        if (image == null) {
            throw new NullPointerException("There is not an image set to the Texture2D");
        }

        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        IntBuffer adaptedImageData = getDataAsIntBuffer(image);

        JmeCursor jmeCursor = new JmeCursor();
        jmeCursor.setWidth(imageWidth);
        jmeCursor.setHeight(imageHeight);
        jmeCursor.setxHotSpot(0);
        jmeCursor.setyHotSpot(imageHeight);
        jmeCursor.setNumImages(1);
        jmeCursor.setImagesDelay(null);
        jmeCursor.setImagesData(adaptedImageData);
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
}