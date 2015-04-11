package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.texture.Image.Format;
import java.util.HashMap;
import java.util.Map;

/**
 * This class creates a pixel IO object for the specified image format.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class PixelIOFactory {
    private static final Map<Format, PixelInputOutput> PIXEL_INPUT_OUTPUT = new HashMap<Format, PixelInputOutput>();

    /**
     * This method returns pixel IO object for the specified format.
     * 
     * @param format
     *            the format of the image
     * @return pixel IO object
     */
    public static PixelInputOutput getPixelIO(Format format) {
        PixelInputOutput result = PIXEL_INPUT_OUTPUT.get(format);
        if (result == null) {
            switch (format) {
                case ABGR8:
                case RGBA8:
                case BGR8:
                case BGRA8:
                case RGB8:
                case ARGB8:
                case RGB111110F:
                case RGB16F:
                case RGB16F_to_RGB111110F:
                case RGB16F_to_RGB9E5:
                case RGB32F:
                case RGB565:
                case RGB5A1:
                case RGB9E5:
                case RGBA16F:
                case RGBA32F:
                    result = new AWTPixelInputOutput();
                    break;
                case Luminance8:
                case Luminance16F:
                case Luminance16FAlpha16F:
                case Luminance32F:
                case Luminance8Alpha8:
                    result = new LuminancePixelInputOutput();
                    break;
                case DXT1:
                case DXT1A:
                case DXT3:
                case DXT5:
                    result = new DDSPixelInputOutput();
                    break;
                default:
                    throw new IllegalStateException("Unsupported image format for IO operations: " + format);
            }
            synchronized (PIXEL_INPUT_OUTPUT) {
                PIXEL_INPUT_OUTPUT.put(format, result);
            }
        }
        return result;
    }
}
