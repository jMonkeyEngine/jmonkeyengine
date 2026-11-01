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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.FrameBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reapplying MRT settings must not introduce render-state changes. */
public class GlRendererMrtCompatibilityTest extends FrameBufferTestSupport {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reapplyingCurrentModePreservesCleanState(boolean mrt) {
        FrameBuffer fb = framebuffer(texture(), texture());
        fb.setMultiTarget(mrt);
        renderer.setFrameBuffer(fb);
        renderer.setViewPort(2, 3, 4, 5);
        final int checks = driver.framebufferChecks;
        fb.setMultiTarget(mrt);
        assertFalse(fb.isUpdateNeeded(), "Reapplying the same mode must remain a no-op");
        renderer.setFrameBuffer(fb);
        assertEquals(checks, driver.framebufferChecks);
        assertEquals(4, driver.viewportWidth);
        assertEquals(5, driver.viewportHeight);
        renderer.cleanup();
    }

    @Test
    void disablingMrtStillSelectsFirstTargetFromAnotherSingleTarget() {
        FrameBuffer fb = framebuffer(texture(), texture());
        fb.setTargetIndex(1);
        renderer.setFrameBuffer(fb);
        fb.setMultiTarget(false);
        assertEquals(0, fb.getTargetIndex());
        assertTrue(fb.isUpdateNeeded());
        renderer.setFrameBuffer(fb);
        assertEquals(java.util.Collections.singletonList(GLFbo.GL_COLOR_ATTACHMENT0_EXT),
                driver.drawBuffers.get(fb.getId()));
        renderer.cleanup();
    }

    @Test
    void reapplyingModeDoesNotClearOtherPendingChanges() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture()));
        fb.setMultiTarget(false);
        assertTrue(fb.isUpdateNeeded());
        renderer.setFrameBuffer(fb);
        assertTrue(driver.attachment(fb, 1) > 0);
        renderer.cleanup();
    }
}
