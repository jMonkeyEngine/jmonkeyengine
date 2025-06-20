package com.jme3.math;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author capdevon
 */
public class ColorRGBATest {

    @Test
    public void testIntColor() {
        float r = 1;
        float g = 0.2f;
        float b = 0.6f;
        float a = 0.8f;
        ColorRGBA color = new ColorRGBA(r, g, b, a);

        int rgba = color.asIntRGBA();
        int abgr = color.asIntABGR();
        int argb = color.asIntARGB();

        Assert.assertEquals(-13395508, rgba);
        Assert.assertEquals(-862374913, abgr);
        Assert.assertEquals(-855690343, argb);

        validateColor(new ColorRGBA().fromIntRGBA(rgba), r, g, b, a);
        validateColor(new ColorRGBA().fromIntABGR(abgr), r, g, b, a);
        validateColor(new ColorRGBA().fromIntARGB(argb), r, g, b, a);
    }

    private void validateColor(ColorRGBA color, float r, float g, float b, float a) {
        Assert.assertEquals(r, color.r, 0.001f);
        Assert.assertEquals(g, color.g, 0.001f);
        Assert.assertEquals(b, color.b, 0.001f);
        Assert.assertEquals(a, color.a, 0.001f);
    }
}
