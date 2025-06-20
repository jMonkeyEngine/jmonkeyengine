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

        Assert.assertEquals(color, copy.fromIntRGBA(rgba));
        Assert.assertEquals(color, copy.fromIntABGR(abgr));
        Assert.assertEquals(color, copy.fromIntARGB(argb));
    }

}
