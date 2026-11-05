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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class GlRendererGpuTimerStateTest {
    @Test
    void rendererDefaultDoesNotRequireDisjointStateSupport() {
        Renderer renderer = mock(Renderer.class, CALLS_REAL_METHODS);
        assertFalse(renderer.isGpuTimingDisjoint());
    }

    @Test
    void bindingDefaultDoesNotRequireDisjointStateSupport() {
        GL binding = mock(GL.class, CALLS_REAL_METHODS);
        assertFalse(binding.isGpuTimingDisjoint());
    }

    @Test
    void rejectsUnsupportedTimerStateBeforeCallingTheBinding() {
        TimerState binding = new TimerState();
        GLRenderer renderer = new GLRenderer(binding.gl(), null, null);
        assertThrows(RendererException.class, renderer::isGpuTimingDisjoint);
        assertEquals(0, binding.checks);
    }

    @Test
    void exposesEachBindingStateCheckIncludingAResetDisjointEvent() {
        TimerState binding = new TimerState();
        GLRenderer renderer = new GLRenderer(binding.gl(), null, null);
        renderer.getCaps().add(Caps.GpuTimerQuery);
        renderer.getCaps().add(Caps.OpenGLES20);
        assertFalse(renderer.isGpuTimingDisjoint());
        binding.disjoint = true;
        assertTrue(renderer.isGpuTimingDisjoint());
        assertFalse(renderer.isGpuTimingDisjoint());
        assertEquals(3, binding.checks);
    }

    @Test
    void preservesAnUnavailableValidationFailureInsteadOfClaimingValidTimings() {
        TimerState binding = new TimerState();
        binding.unavailable = true;
        GLRenderer renderer = new GLRenderer(binding.gl(), null, null);
        renderer.getCaps().add(Caps.GpuTimerQuery);
        renderer.getCaps().add(Caps.OpenGLES20);
        assertThrows(UnsupportedOperationException.class, renderer::isGpuTimingDisjoint);
    }

    @Test
    void desktopOpenGlHasNoDisjointFlagAndDoesNotCallAGlesBinding() {
        TimerState binding = new TimerState();
        binding.unavailable = true;
        GLRenderer renderer = new GLRenderer(binding.gl(), null, null);
        renderer.getCaps().add(Caps.GpuTimerQuery);
        assertFalse(renderer.isGpuTimingDisjoint());
        assertEquals(0, binding.checks);
    }

    private static final class TimerState {
        boolean disjoint;
        boolean unavailable;
        int checks;

        GL gl() {
            return (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), new Class<?>[]{GL.class},
                    (proxy, method, arguments) -> {
                        if (!method.getName().equals("isGpuTimingDisjoint")) {
                            throw new UnsupportedOperationException("Unexpected GL call: " + method.getName());
                        }
                        checks++;
                        if (unavailable) {
                            throw new UnsupportedOperationException("No timer-state validation");
                        }
                        boolean event = disjoint;
                        disjoint = false;
                        return event;
                    });
        }
    }
}
