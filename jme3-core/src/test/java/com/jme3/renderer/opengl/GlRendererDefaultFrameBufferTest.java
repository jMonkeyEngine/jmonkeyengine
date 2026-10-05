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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jme3.texture.FrameBuffer;
import org.junit.jupiter.api.Test;

/** Tests the platform presentation framebuffer when its name is nonzero. */
public class GlRendererDefaultFrameBufferTest extends FrameBufferTestSupport {
    @Test
    void copyingToScreenUsesDiscoveredFramebuffer() {
        initializeRenderer(77);
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        assertEquals(77, driver.drawFramebuffer);
        renderer.copyFrameBuffer(fb, null, true, false);
        assertEquals(77, driver.blitDrawFramebuffer);
    }

    @Test
    void copyingFromScreenUsesDiscoveredFramebuffer() {
        initializeRenderer(77);
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        assertEquals(77, driver.readFramebuffer);
        renderer.copyFrameBuffer(null, fb, true, false);
        assertEquals(77, driver.blitReadFramebuffer);
    }

    @Test
    void zeroDefaultCopyRestoresPreviousFramebuffer() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.copyFrameBuffer(fb, null, true, false);
        assertEquals(0, driver.blitDrawFramebuffer);
        assertEquals(fb.getId(), driver.drawFramebuffer);
        assertEquals(fb.getId(), driver.readFramebuffer);
    }

    @Test
    void copyImmediatelyAfterInitializationRestoresDiscoveredFramebuffer() {
        initializeRenderer(77);
        FrameBuffer fb = framebuffer(texture());
        renderer.copyFrameBuffer(fb, null, true, false);
        assertEquals(77, driver.blitDrawFramebuffer);
        assertEquals(77, driver.drawFramebuffer);
        assertEquals(77, driver.readFramebuffer);
        assertNull(renderer.getCurrentFrameBuffer());
    }

    @Test
    void deletingBoundFramebufferRestoresScreenAndClearsObjectCache() {
        initializeRenderer(77);
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        renderer.deleteFrameBuffer(fb);
        assertEquals(77, driver.drawFramebuffer);
        assertEquals(77, driver.readFramebuffer);
        assertNull(renderer.getCurrentFrameBuffer());
        renderer.setFrameBuffer(null);
        assertEquals(77, driver.drawFramebuffer);
    }

    @Test
    void mainOverrideIsBoundWhenDiscoveredDefaultIsCurrent() {
        initializeRenderer(77);
        FrameBuffer override = framebuffer(texture());
        renderer.setMainFrameBufferOverride(override);
        assertEquals(override.getId(), driver.drawFramebuffer);
        renderer.copyFrameBuffer(null, null, true, false);
        assertEquals(override.getId(), driver.blitReadFramebuffer);
        assertEquals(override.getId(), driver.blitDrawFramebuffer);
    }
}
