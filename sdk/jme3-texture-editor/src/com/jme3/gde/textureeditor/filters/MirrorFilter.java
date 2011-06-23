package com.jme3.gde.textureeditor.filters;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class MirrorFilter implements BufferedImageFilter {

    public static final Integer X = 0;
    public static final Integer Y = 1;

    public static MirrorFilter create() {
        return new MirrorFilter();
    }

    protected MirrorFilter() {
    }

    public BufferedImage filter(BufferedImage source, Object... args) {
        if (args[0] == Y) {
            AffineTransform op = AffineTransform.getScaleInstance(1, -1);
            op.translate(0, -source.getHeight(null));
            return new AffineTransformOp(op, null).filter(source, null);
        } else if (args[0] == X) {
            AffineTransform op = AffineTransform.getScaleInstance(-1, 1);
            op.translate(-source.getWidth(null), 0);
            return new AffineTransformOp(op, null).filter(source, null);
        } else {
            throw new IllegalArgumentException("MirrorFilter requires MirrorFilter.X or MirrorFilter.Y as argument");
        }
    }
}
