package com.jme3.texture.image;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;

public class DefaultImageRaster extends ImageRaster {
    
    private final int[] components = new int[4];
    private final ByteBuffer buffer;
    private final Image image;
    private final ImageCodec codec;
    private final int width;
    private final int height;
    private final byte[] temp;
    
    private void rangeCheck(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("x and y must be inside the image dimensions");
        }
    }
    
    public DefaultImageRaster(Image image, int slice) {
        this.image = image;
        this.buffer = image.getData(slice);
        this.codec = ImageCodec.lookup(image.getFormat());
        this.width = image.getWidth();
        this.height = image.getHeight();
        if (codec instanceof ByteAlignedImageCodec) {
            this.temp = new byte[codec.bpp];
        } else {
            this.temp = null;
        }
    }
    
    @Override
    public void setPixel(int x, int y, ColorRGBA color) {
        rangeCheck(x, y);
        
        // Check flags for grayscale
        if ((codec.flags & ImageCodec.FLAG_GRAY) != 0) {
            float gray = color.r * 0.27f + color.g * 0.67f + color.b * 0.06f;
            color = new ColorRGBA(gray, gray, gray, color.a);
        }

        if ((codec.flags & ImageCodec.FLAG_F16) != 0) {
            components[0] = (int) FastMath.convertFloatToHalf(color.a);
            components[1] = (int) FastMath.convertFloatToHalf(color.r);
            components[2] = (int) FastMath.convertFloatToHalf(color.g);
            components[3] = (int) FastMath.convertFloatToHalf(color.b);
        } else if ((codec.flags & ImageCodec.FLAG_F32) != 0) {
            components[0] = (int) Float.floatToIntBits(color.a);
            components[1] = (int) Float.floatToIntBits(color.r);
            components[2] = (int) Float.floatToIntBits(color.g);
            components[3] = (int) Float.floatToIntBits(color.b);
        } else {
            // Convert color to bits by multiplying by size
            components[0] = Math.min( (int) (color.a * codec.maxAlpha + 0.5f), codec.maxAlpha);
            components[1] = Math.min( (int) (color.r * codec.maxRed + 0.5f), codec.maxRed);
            components[2] = Math.min( (int) (color.g * codec.maxGreen + 0.5f), codec.maxGreen);
            components[3] = Math.min( (int) (color.b * codec.maxBlue + 0.5f), codec.maxBlue);
        }

        codec.writeComponents(buffer, x, y, width, components, temp);
        
        image.setUpdateNeeded();
    }
    
    @Override
    public ColorRGBA getPixel(int x, int y, ColorRGBA store) {
        rangeCheck(x, y);
        
        codec.readComponents(buffer, x, y, width, components, temp);
     
        if (store == null) {
            store = new ColorRGBA();
        }
        if ((codec.flags & ImageCodec.FLAG_F16) != 0) {
            store.set(FastMath.convertHalfToFloat((short)components[1]),
                      FastMath.convertHalfToFloat((short)components[2]),
                      FastMath.convertHalfToFloat((short)components[3]),
                      FastMath.convertHalfToFloat((short)components[0]));
        } else if ((codec.flags & ImageCodec.FLAG_F32) != 0) {
            store.set(Float.intBitsToFloat((int)components[1]),
                      Float.intBitsToFloat((int)components[2]),
                      Float.intBitsToFloat((int)components[3]),
                      Float.intBitsToFloat((int)components[0]));
        } else {
            // Convert to float and divide by bitsize to get into range 0.0 - 1.0.
            store.set((float)components[1] / codec.maxRed,
                      (float)components[2] / codec.maxGreen,
                      (float)components[3] / codec.maxBlue,
                      (float)components[0] / codec.maxAlpha);
        }
        if ((codec.flags & ImageCodec.FLAG_GRAY) != 0) {
            store.g = store.b = store.r;
        } else {
            if (codec.maxRed == 0) {
                store.r = 1;
            }
            if (codec.maxGreen == 0) {
                store.g = 1;
            }
            if (codec.maxBlue == 0) {
                store.b = 1;
            }
            if (codec.maxAlpha == 0) {
                store.a = 1;
            }
        }
        return store;
    }
}
