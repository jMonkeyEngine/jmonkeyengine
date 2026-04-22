package com.jme3.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        Assertions.assertEquals(-13395508, rgba);
        Assertions.assertEquals(-862374913, abgr);
        Assertions.assertEquals(-855690343, argb);

        ColorRGBA copy = new ColorRGBA();

        Assertions.assertEquals(color, copy.fromIntRGBA(rgba));
        Assertions.assertEquals(color, copy.fromIntABGR(abgr));
        Assertions.assertEquals(color, copy.fromIntARGB(argb));
    }

}
