package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;

/**
 * Implemens read/write operations for luminance images.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class LuminancePixelInputOutput implements PixelInputOutput {
    public void read(Image image, int layer, TexturePixel pixel, int index) {
        ByteBuffer data = image.getData(layer);
        switch (image.getFormat()) {
            case Luminance8:
                pixel.fromIntensity(data.get(index));
                break;
            case Luminance8Alpha8:
                pixel.fromIntensity(data.get(index));
                pixel.setAlpha(data.get(index + 1));
                break;
            case Luminance16F:
                pixel.intensity = FastMath.convertHalfToFloat(data.getShort(index));
                break;
            case Luminance16FAlpha16F:
                pixel.intensity = FastMath.convertHalfToFloat(data.getShort(index));
                pixel.alpha = FastMath.convertHalfToFloat(data.getShort(index + 2));
                break;
            case Luminance32F:
                pixel.intensity = Float.intBitsToFloat(data.getInt(index));
                break;
            default:
                throw new IllegalStateException("Unknown luminance format type.");
        }
    }

    public void read(Image image, int layer, TexturePixel pixel, int x, int y) {
        int index = y * image.getWidth() + x;
        this.read(image, layer, pixel, index);
    }

    public void write(Image image, int layer, TexturePixel pixel, int index) {
        ByteBuffer data = image.getData(layer);
        data.put(index, pixel.getInt());
        switch (image.getFormat()) {
            case Luminance8:
                data.put(index, pixel.getInt());
                break;
            case Luminance8Alpha8:
                data.put(index, pixel.getInt());
                data.put(index + 1, pixel.getA8());
                break;
            case Luminance16F:
                data.putShort(index, FastMath.convertFloatToHalf(pixel.intensity));
                break;
            case Luminance16FAlpha16F:
                data.putShort(index, FastMath.convertFloatToHalf(pixel.intensity));
                data.putShort(index + 2, FastMath.convertFloatToHalf(pixel.alpha));
                break;
            case Luminance32F:
                data.putInt(index, Float.floatToIntBits(pixel.intensity));
                break;
            default:
                throw new IllegalStateException("Unknown luminance format type.");
        }
    }

    public void write(Image image, int layer, TexturePixel pixel, int x, int y) {
        int index = y * image.getWidth() + x;
        this.write(image, layer, pixel, index);
    }
}
