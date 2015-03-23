package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;
import jme3tools.converters.RGB565;

/**
 * Implemens read/write operations for AWT images.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class AWTPixelInputOutput implements PixelInputOutput {
    public void read(Image image, int layer, TexturePixel pixel, int index) {
        ByteBuffer data = image.getData(layer);
        switch (image.getFormat()) {
            case RGBA8:
                pixel.fromARGB8(data.get(index + 3), data.get(index), data.get(index + 1), data.get(index + 2));
                break;
            case ARGB8:
                pixel.fromARGB8(data.get(index), data.get(index + 1), data.get(index + 2), data.get(index + 3));
                break;
            case ABGR8:
                pixel.fromARGB8(data.get(index), data.get(index + 3), data.get(index + 2), data.get(index + 1));
                break;
            case BGR8:
                pixel.fromARGB8((byte) 0xFF, data.get(index + 2), data.get(index + 1), data.get(index));
                break;
            case BGRA8:
                pixel.fromARGB8(data.get(index + 3), data.get(index + 2), data.get(index + 1), data.get(index));
                break;
            case RGB8:
                pixel.fromARGB8((byte) 0xFF, data.get(index), data.get(index + 1), data.get(index + 2));
                break;
            case RGB565:
                pixel.fromARGB8(RGB565.RGB565_to_ARGB8(data.getShort(index)));
                break;
            case RGB5A1:
                short rgb5a1 = data.getShort(index);
                byte a = (byte) (rgb5a1 & 0x01);
                int r = (rgb5a1 & 0xf800) >> 11 << 3;
                int g = (rgb5a1 & 0x07c0) >> 6 << 3;
                int b = (rgb5a1 & 0x001f) >> 1 << 3;
                pixel.fromARGB8(a == 1 ? (byte) 255 : 0, (byte) r, (byte) g, (byte) b);
                break;
            case RGB16F:
            case RGB16F_to_RGB111110F:
            case RGB16F_to_RGB9E5:
                pixel.fromARGB(1, FastMath.convertHalfToFloat(data.getShort(index)), FastMath.convertHalfToFloat(data.getShort(index + 2)), FastMath.convertHalfToFloat(data.getShort(index + 4)));
                break;
            case RGBA16F:
                pixel.fromARGB(FastMath.convertHalfToFloat(data.getShort(index + 6)), FastMath.convertHalfToFloat(data.getShort(index)), FastMath.convertHalfToFloat(data.getShort(index + 2)), FastMath.convertHalfToFloat(data.getShort(index + 4)));
                break;
            case RGBA32F:
                pixel.fromARGB(Float.intBitsToFloat(data.getInt(index + 12)), Float.intBitsToFloat(data.getInt(index)), Float.intBitsToFloat(data.getInt(index + 4)), Float.intBitsToFloat(data.getInt(index + 8)));
                break;
            case RGB111110F:// the data is stored as 32-bit unsigned int, that is why we cast the read data to long and remove MSB-bytes to get the positive value
                pixel.fromARGB(1, (float) Double.longBitsToDouble((long) data.getInt(index) & 0x00000000FFFFFFFF), (float) Double.longBitsToDouble((long) data.getInt(index + 4) & 0x00000000FFFFFFFF), (float) Double.longBitsToDouble((long) data.getInt(index + 8) & 0x00000000FFFFFFFF));
                break;
            case RGB9E5:// TODO: support these
                throw new IllegalStateException("Not supported image type for IO operations: " + image.getFormat());
            default:
                throw new IllegalStateException("Unknown image format: " + image.getFormat());
        }
    }

    public void read(Image image, int layer, TexturePixel pixel, int x, int y) {
        int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
        this.read(image, layer, pixel, index);
    }

    public void write(Image image, int layer, TexturePixel pixel, int index) {
        ByteBuffer data = image.getData(layer);
        switch (image.getFormat()) {
            case RGBA8:
                data.put(index, pixel.getR8());
                data.put(index + 1, pixel.getG8());
                data.put(index + 2, pixel.getB8());
                data.put(index + 3, pixel.getA8());
                break;
            case ARGB8:
                data.put(index, pixel.getA8());
                data.put(index + 1, pixel.getR8());
                data.put(index + 2, pixel.getG8());
                data.put(index + 3, pixel.getB8());
                break;
            case ABGR8:
                data.put(index, pixel.getA8());
                data.put(index + 1, pixel.getB8());
                data.put(index + 2, pixel.getG8());
                data.put(index + 3, pixel.getR8());
                break;
            case BGR8:
                data.put(index, pixel.getB8());
                data.put(index + 1, pixel.getG8());
                data.put(index + 2, pixel.getR8());
                break;
            case BGRA8:
                data.put(index, pixel.getB8());
                data.put(index + 1, pixel.getG8());
                data.put(index + 2, pixel.getR8());
                data.put(index + 3, pixel.getA8());
                break;
            case RGB8:
                data.put(index, pixel.getR8());
                data.put(index + 1, pixel.getG8());
                data.put(index + 2, pixel.getB8());
                break;
            case RGB565:
                data.putShort(RGB565.ARGB8_to_RGB565(pixel.toARGB8()));
                break;
            case RGB5A1:
                int argb8 = pixel.toARGB8();
                short r = (short) ((argb8 & 0x00F80000) >> 8);
                short g = (short) ((argb8 & 0x0000F800) >> 5);
                short b = (short) ((argb8 & 0x000000F8) >> 2);
                short a = (short) ((short) ((argb8 & 0xFF000000) >> 24) > 0 ? 1 : 0);
                data.putShort(index, (short) (r | g | b | a));
                break;
            case RGB16F:
            case RGB16F_to_RGB111110F:
            case RGB16F_to_RGB9E5:
                data.putShort(index, FastMath.convertFloatToHalf(pixel.red));
                data.putShort(index + 2, FastMath.convertFloatToHalf(pixel.green));
                data.putShort(index + 4, FastMath.convertFloatToHalf(pixel.blue));
                break;
            case RGBA16F:
                data.putShort(index, FastMath.convertFloatToHalf(pixel.red));
                data.putShort(index + 2, FastMath.convertFloatToHalf(pixel.green));
                data.putShort(index + 4, FastMath.convertFloatToHalf(pixel.blue));
                data.putShort(index + 6, FastMath.convertFloatToHalf(pixel.blue));
                break;
            case RGB32F:
            case RGB111110F:// this data is stored as 32-bit unsigned int
                data.putInt(index, Float.floatToIntBits(pixel.red));
                data.putInt(index + 2, Float.floatToIntBits(pixel.green));
                data.putInt(index + 4, Float.floatToIntBits(pixel.blue));
                break;
            case RGBA32F:
                data.putInt(index, Float.floatToIntBits(pixel.red));
                data.putInt(index + 2, Float.floatToIntBits(pixel.green));
                data.putInt(index + 4, Float.floatToIntBits(pixel.blue));
                data.putInt(index + 6, Float.floatToIntBits(pixel.alpha));
                break;
            case RGB9E5:// TODO: support these
                throw new IllegalStateException("Not supported image type for IO operations: " + image.getFormat());
            default:
                throw new IllegalStateException("Unknown image format: " + image.getFormat());
        }
    }

    public void write(Image image, int layer, TexturePixel pixel, int x, int y) {
        int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
        this.write(image, layer, pixel, index);
    }
}
