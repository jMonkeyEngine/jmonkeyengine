package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;

import java.nio.Buffer;

/**
 * The base class of LWJGL implementations.
 *
 * @author JavaSaBr
 */
public class LwjglRender {

    protected static void checkLimit(final Buffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }
}
