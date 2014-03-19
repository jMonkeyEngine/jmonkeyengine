package com.jme3.gde.textureeditor.filters;

import java.awt.image.BufferedImage;

/**
 * Texture Mapper
 *
 * This class will take a texture and will map it to a spherical
 * adaptation so no, at least no visible, distortion is apparent.
 *
 * @author MadJack
 * @created 06/18/2011
 */
public class SphereMappedFilter implements BufferedImageFilter {

    public static SphereMappedFilter create() {
        return new SphereMappedFilter();
    }

    protected SphereMappedFilter() {
    }

    /*
     * The following algorithm is heavily based on
     * Paul Bourke's pseudo code available at:
     * http://paulbourke.net/texture_colour/texturemap/
     *
     */
    @Override
    public BufferedImage filter(BufferedImage sourceImg, Object... args) {

        BufferedImage imageOut = new BufferedImage(sourceImg.getWidth(), sourceImg.getHeight(), BufferedImage.TYPE_INT_ARGB);

        double theta, phi, phi2;
        int i, i2, j;

        for (j = 0; j < sourceImg.getHeight(); j++) {
            theta = Math.PI * (j - (sourceImg.getHeight() - 1) / 2.0f) / (sourceImg.getHeight() - 1);
            for (i = 0; i < sourceImg.getWidth(); i++) {
                phi = Math.PI * 2 * (i - sourceImg.getWidth() / 2.0f) / sourceImg.getWidth();
                phi2 = phi * Math.cos(theta);
                i2 = (int) Math.round(phi2 * sourceImg.getWidth() / (Math.PI * 2) + sourceImg.getWidth() / 2);

                int newpixel = 0;
                if (i2 < 0 || i2 > sourceImg.getWidth() - 1) {
                    /* Should not happen, make that a red pixel */
                    newpixel = 100;
                } else {
                    newpixel = sourceImg.getRGB(i2, j);
                }
                imageOut.setRGB(i, j, newpixel);
            }
        }
        return imageOut;
    }
}
