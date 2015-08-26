package com.jme3.system.lwjgl;

import com.jme3.system.JmeContext;

/**
 * @author Daniel Johansson
 * @since 2015-08-11
 */
public class LwjglOffscreenBuffer extends LwjglWindow {

    public LwjglOffscreenBuffer() {
        super(JmeContext.Type.OffscreenSurface);
    }
}
