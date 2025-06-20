package com.jme3.math;

import org.junit.Assert;
import org.junit.Test;

public class ColorRGBATest {

    @Test
    public void testIntColor() {
        float r = 1,
                g = 0.2f,
                b = 0.6f,
                a = 0.8f;

        ColorRGBA color = new ColorRGBA(r, g, b, a);

        int rgba = color.asIntRGBA();
        int abgr = color.asIntABGR();
        int argb = color.asIntARGB();

        Assert.assertEquals(-13395508, rgba);
        Assert.assertEquals(-862374913, abgr);
        Assert.assertEquals(-855690343, argb);

        validateColor(new ColorRGBA().fromIntRGBA(rgba), 1.0f, 0.2f, 0.6f, 0.8f);
        validateColor(new ColorRGBA().fromIntABGR(abgr), 1.0f, 0.2f, 0.6f, 0.8f);
        validateColor(new ColorRGBA().fromIntARGB(argb), 1.0f, 0.2f, 0.6f, 0.8f);
    }

    private void validateColor(ColorRGBA color, float r, float g, float b, float a) {
        Assert.assertEquals(r, color.r, 0.001f);
        Assert.assertEquals(g, color.g, 0.001f);
        Assert.assertEquals(b, color.b, 0.001f);
        Assert.assertEquals(a, color.a, 0.001f);
    }
}
