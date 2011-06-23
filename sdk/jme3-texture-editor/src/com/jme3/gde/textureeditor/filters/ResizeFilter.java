package com.jme3.gde.textureeditor.filters;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class ResizeFilter implements BufferedImageFilter {

    public static ResizeFilter create() {
        return new ResizeFilter();
    }

    protected ResizeFilter() {
    }

    public BufferedImage filter(BufferedImage source, Object... args) {
        int newWidth = (Integer) args[0];
        int newHeight = (Integer) args[1];
        int type = source.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, type);
        scaled.getGraphics().drawImage(
                source.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH),
                0, 0, null);
        return scaled;
    }
}
