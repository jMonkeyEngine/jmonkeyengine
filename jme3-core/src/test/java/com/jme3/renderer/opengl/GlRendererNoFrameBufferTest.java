/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.opengl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/** Screen rendering remains available on OpenGL 2.1 without FBO extensions. */
public class GlRendererNoFrameBufferTest extends FrameBufferTestSupport {
    private int framebufferQueries;

    private void initializeWithoutFramebuffers(boolean rejectBindingQuery) {
        driver = new RecordingGL(0);
        GL2 legacyGl = (GL2) Proxy.newProxyInstance(GL2.class.getClassLoader(),
                new Class<?>[] {GL2.class}, (proxy, method, args) -> {
                    if (method.getName().equals("glGetString") && (Integer) args[0] == GL.GL_EXTENSIONS) {
                        return "";
                    }
                    if (method.getName().equals("glGetInteger")
                            && (Integer) args[0] == GL.GL_FRAMEBUFFER_BINDING) {
                        framebufferQueries++;
                        if (rejectBindingQuery) {
                            throw new AssertionError("GL_FRAMEBUFFER_BINDING is not supported");
                        }
                    }
                    return driver.invoke(proxy, method, args);
                });
        GLFbo unsupportedFbo = (GLFbo) Proxy.newProxyInstance(GLFbo.class.getClassLoader(),
                new Class<?>[] {GLFbo.class}, (proxy, method, args) -> {
                    throw new AssertionError("Unsupported FBO entry point: " + method.getName());
                });
        renderer = new GLRenderer(legacyGl, driver.proxy(GLExt.class), unsupportedFbo);
        renderer.initialize();
        assertFalse(renderer.getCaps().contains(Caps.FrameBuffer));
    }

    @Test
    void initializationDoesNotQueryUnsupportedFramebufferBinding() {
        assertDoesNotThrow(() -> initializeWithoutFramebuffers(true));
        assertEquals(0, framebufferQueries);
    }

    @Test
    void screenBindingWorksBeforeAndAfterInvalidation() {
        initializeWithoutFramebuffers(false);
        renderer.setFrameBuffer(null);
        renderer.invalidateState();
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
        assertNull(renderer.getCurrentFrameBuffer());
        assertEquals(0, driver.drawFramebuffer);
    }

    @Test
    void screenBindingWorksAfterContextResetAndCleanup() {
        initializeWithoutFramebuffers(false);
        renderer.resetGLObjects();
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
        renderer.cleanup();
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
        assertNull(renderer.getCurrentFrameBuffer());
    }

    @Test
    void screenReadbackWorksAfterInvalidation() {
        initializeWithoutFramebuffers(false);
        renderer.setViewPort(0, 0, 1, 1);
        renderer.invalidateState();
        assertDoesNotThrow(() -> renderer.readFrameBuffer(null, BufferUtils.createByteBuffer(4)));
    }

    @Test
    void clearingMainOverrideDoesNotRequireFramebufferFunctions() {
        initializeWithoutFramebuffers(false);
        renderer.invalidateState();
        assertDoesNotThrow(() -> renderer.setMainFrameBufferOverride(null));
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
    }

    @Test
    void offscreenFramebufferRemainsUnsupportedWithoutAllocation() {
        initializeWithoutFramebuffers(false);
        FrameBuffer offscreen = framebuffer(texture());
        renderer.invalidateState();
        assertThrows(RendererException.class, () -> renderer.setFrameBuffer(offscreen));
        assertEquals(-1, offscreen.getId());
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
    }

    @Test
    void offscreenOverrideRemainsUnsupportedWithoutAllocation() {
        initializeWithoutFramebuffers(false);
        FrameBuffer offscreen = framebuffer(texture());
        renderer.invalidateState();
        assertThrows(RendererException.class, () -> renderer.setMainFrameBufferOverride(offscreen));
        assertEquals(-1, offscreen.getId());
        assertNull(renderer.getCurrentFrameBuffer());
        assertDoesNotThrow(() -> renderer.setFrameBuffer(null));
    }
}
