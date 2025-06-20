package com.jme3.math;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author capdevon
 */
public class ColorRGBATest {

    @Test
    public void testIntColor() {
        ColorRGBA color = new ColorRGBA(1.0f, 0.2f, 0.6f, 0.8f);

        int rgba = color.asIntRGBA();
        int abgr = color.asIntABGR();
        int argb = color.asIntARGB();

        Assert.assertEquals(-13395508, rgba);
        Assert.assertEquals(-862374913, abgr);
        Assert.assertEquals(-855690343, argb);

        ColorRGBA copy = new ColorRGBA();

        validateColor(color, copy.fromIntRGBA(rgba));
        validateColor(color, copy.fromIntABGR(abgr));
        validateColor(color, copy.fromIntARGB(argb));
    }

    private void validateColor(ColorRGBA original, ColorRGBA copy) {
        Assert.assertEquals(original.r, copy.r, 0.001f);
        Assert.assertEquals(original.g, copy.g, 0.001f);
        Assert.assertEquals(original.b, copy.b, 0.001f);
        Assert.assertEquals(original.a, copy.a, 0.001f);
    }
}
