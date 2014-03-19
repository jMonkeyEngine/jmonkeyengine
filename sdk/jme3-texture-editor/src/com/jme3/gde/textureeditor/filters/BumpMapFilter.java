package com.jme3.gde.textureeditor.filters;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class BumpMapFilter implements BufferedImageFilter {

    private static class Vec3f {

        float x, y, z;

        void divideLocal(float d) {
            x /= d;
            y /= d;
            z /= d;
        }
    };

    public static BumpMapFilter create() {
        return new BumpMapFilter();
    }

    protected BumpMapFilter() {
    }

    public BufferedImage filter(BufferedImage sourceImage, Object... args) {
        float a = (Float) args[0];
        BufferedImage heightMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage bumpMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp gscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        gscale.filter(sourceImage, heightMap);
        for (int x = 0; x < bumpMap.getWidth(); x++) {
            for (int y = 0; y < bumpMap.getHeight(); y++) {
                bumpMap.setRGB(x, y, generateBumpPixel(heightMap, x, y, a));
            }
        }
        return bumpMap;
    }

    public int generateBumpPixel(BufferedImage image, int x, int y, float a) {
        Vec3f S = new Vec3f();
        Vec3f T = new Vec3f();
        Vec3f N = new Vec3f();

        S.x = 1;
        S.y = 0;
        S.z = a * getHeight(image, x + 1, y) - a * getHeight(image, x - 1, y);
        T.x = 0;
        T.y = 1;
        T.z = a * getHeight(image, x, y + 1) - a * getHeight(image, x, y - 1);

        float den = (float) Math.sqrt(S.z * S.z + T.z * T.z + 1);
        N.x = -S.z;
        N.y = -T.z;
        N.z = 1;
        N.divideLocal(den);
        return vectorToColor(N.x, N.y, N.z);
    }

    private float getHeight(BufferedImage image, int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x >= image.getWidth()) {
            x = image.getWidth() - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= image.getHeight()) {
            y = image.getHeight() - 1;
        }
        return image.getRGB(x, y) & 0xff;
    }

    public int vectorToColor(float x, float y, float z) {
        int r = Math.round(255 * ((x + 1f) / 2f));
        int g = Math.round(255 * ((y + 1f) / 2f));
        int b = Math.round(255 * ((z + 1f) / 2f));
        return (255 << 24) + (r << 16) + (g << 8) + b;
    }

    @Override
    public String toString() {
        return "Bump Map";
    }
}
