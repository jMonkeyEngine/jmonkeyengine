package com.jme3.gde.textureeditor.filters;

import java.awt.image.BufferedImage;

public interface BufferedImageFilter {

    BufferedImage filter(BufferedImage source, Object... args);
}
