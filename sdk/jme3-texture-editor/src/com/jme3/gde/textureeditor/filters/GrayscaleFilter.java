package com.jme3.gde.textureeditor.filters;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class GrayscaleFilter implements BufferedImageFilter {

    public static GrayscaleFilter create() {
        return new GrayscaleFilter();
    }

    protected GrayscaleFilter() {
    }

    public BufferedImage filter(BufferedImage sourceImage, Object... args) {
        BufferedImage filtered = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp gscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        gscale.filter(sourceImage, filtered);
        return filtered;
    }
}
