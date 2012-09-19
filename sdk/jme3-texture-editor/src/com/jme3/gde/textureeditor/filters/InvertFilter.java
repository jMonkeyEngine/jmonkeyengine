package com.jme3.gde.textureeditor.filters;

import java.awt.image.BufferedImage;

/**
 * Invert channels in an image. It takes the ARGB color component and set it to 255 - value.
 */
public class InvertFilter implements BufferedImageFilter {

    public enum Channel {

        All, Red, Green, Blue, Alpha;
    }

    public BufferedImage filter(BufferedImage source, Object... args) {
        final Channel channel;
        if ((args == null) || (args.length < 1) || (!(args[0] instanceof Channel))) {
            channel = Channel.All;
        } else {
            channel = (Channel) args[0];
        }
        BufferedImage result = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final int height = source.getHeight();
        final int width = source.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int sourceARGB = source.getRGB(x, y);
                final int targetARGB = invert(sourceARGB, channel);
                result.setRGB(x, y, targetARGB);
            }
        }
        return result;
    }

    private int invert(final int sourceARGB, final Channel channel) {
        int a = (sourceARGB >> 24) & 0xff;
        int r = (sourceARGB >> 16) & 0xff;
        int g = (sourceARGB >> 8) & 0xff;
        int b = sourceARGB & 0xff;

        switch (channel) {
            case Alpha:
                a = 255 - a;
                break;
            case Red:
                r = 255 - r;
                break;
            case Green:
                g = 255 - g;
                break;
            case Blue:
                b = 255 - b;
                break;
            default:
                a = 255 - a;
                r = 255 - r;
                g = 255 - g;
                b = 255 - b;
        }
        return packPixel(a, r, g, b);
    }

    private int packPixel(int alpha, int red, int green, int blue) {
        final int argb = ((alpha << 24) & 0xff000000)
                | ((red << 16) & 0x00ff0000)
                | ((green << 8) & 0x0000ff00)
                | (blue & 0x000000ff);
        return argb;
    }
}
