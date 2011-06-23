package com.jme3.gde.textureeditor.filters;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class RotateLeftFilter implements BufferedImageFilter {

    public static RotateLeftFilter create() {
        return new RotateLeftFilter();
    }

    protected RotateLeftFilter() {
    }

    public BufferedImage filter(BufferedImage source, Object... args) {
        int type = source.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }
//        BufferedImage dest = new BufferedImage(source.getHeight(), source.getWidth(), type);
        AffineTransform rot = AffineTransform.getRotateInstance(
                Math.PI / 2, source.getWidth() / 2,
                source.getHeight() / 2);
        Point2D p0 = new Point2D.Double();
        Point2D p1 = rot.transform(p0, null);
        double dy = p1.getY();
        p0.setLocation(0, source.getHeight());
        rot.transform(p0, p1);
        double dx = p1.getX();
        AffineTransform trans = AffineTransform.getTranslateInstance(-dx, -dy);
        rot.preConcatenate(trans);
        return new AffineTransformOp(rot, null).filter(source, null);
    }
}
