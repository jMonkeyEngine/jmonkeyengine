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

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jme3.material.RenderState;
import com.jme3.material.RenderState.StencilOperation;
import com.jme3.material.RenderState.TestFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that cached stencil state matches the calls made to OpenGL.
 */
public class GlRendererStencilStateTest {

    private GL gl;
    private GLRenderer renderer;

    @BeforeEach
    public void initializeRenderer() {
        gl = mock(GL.class);
        renderer = new GLRenderer(gl, mock(GLExt.class), mock(GLFbo.class));
    }

    @Test
    public void disablingStencilWithUnchangedParametersDisablesTheTest() {
        renderer.applyRenderState(stencil(true, TestFunction.Never));
        verify(gl).glEnable(GL.GL_STENCIL_TEST);
        clearInvocations(gl);

        renderer.applyRenderState(stencil(false, TestFunction.Never));

        verify(gl).glDisable(GL.GL_STENCIL_TEST);
    }

    @Test
    public void applyingUnchangedEnabledStateDoesNotRepeatGlCalls() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        renderer.applyRenderState(state);

        verifyNoInteractions(gl);
    }

    @Test
    public void stencilCanBeDisabledAndReenabledWithTheSameParameters() {
        RenderState enabled = stencil(true, TestFunction.Never);
        RenderState disabled = stencil(false, TestFunction.Never);
        renderer.applyRenderState(enabled);
        clearInvocations(gl);
        renderer.applyRenderState(disabled);
        verify(gl).glDisable(GL.GL_STENCIL_TEST);
        clearInvocations(gl);

        renderer.applyRenderState(enabled);

        verify(gl).glEnable(GL.GL_STENCIL_TEST);
        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_NEVER, 0, Integer.MAX_VALUE);
        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_NEVER, 0, Integer.MAX_VALUE);
    }

    @Test
    public void changingOnlyTheFrontReferenceUpdatesTheFunction() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setFrontStencilReference(2);
        renderer.applyRenderState(state);

        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_EQUAL, 2, Integer.MAX_VALUE);
    }

    @Test
    public void changingOnlyTheBackReferenceUpdatesTheFunction() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setBackStencilReference(3);
        renderer.applyRenderState(state);

        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_EQUAL, 3, Integer.MAX_VALUE);
    }

    @Test
    public void changingOnlyTheFrontMaskUpdatesTheFunction() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setFrontStencilMask(0x0f);
        renderer.applyRenderState(state);

        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_EQUAL, 0, 0x0f);
    }

    @Test
    public void changingOnlyTheBackMaskUpdatesTheFunction() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setBackStencilMask(0xf0);
        renderer.applyRenderState(state);

        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_EQUAL, 0, 0xf0);
    }

    @Test
    public void changingOnlyTheOperationsUpdatesBothFaces() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setStencil(true,
                StencilOperation.Replace, StencilOperation.Keep, StencilOperation.Increment,
                StencilOperation.Zero, StencilOperation.Decrement, StencilOperation.Invert,
                TestFunction.Equal, TestFunction.Equal);
        renderer.applyRenderState(state);

        verify(gl).glStencilOpSeparate(GL.GL_FRONT, GL.GL_REPLACE, GL.GL_KEEP, GL.GL_INCR);
        verify(gl).glStencilOpSeparate(GL.GL_BACK, GL.GL_ZERO, GL.GL_DECR, GL.GL_INVERT);
    }

    @Test
    public void changingOnlyTheFunctionsUpdatesBothFaces() {
        RenderState state = stencil(true, TestFunction.Equal);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setStencil(true,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                TestFunction.Never, TestFunction.Always);
        renderer.applyRenderState(state);

        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_NEVER, 0, Integer.MAX_VALUE);
        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_ALWAYS, 0, Integer.MAX_VALUE);
    }

    @Test
    public void parametersChangedWhileDisabledAreAppliedWhenEnabled() {
        RenderState state = stencil(false, TestFunction.Equal);
        state.setFrontStencilReference(2);
        state.setBackStencilReference(3);
        state.setFrontStencilMask(0x0f);
        state.setBackStencilMask(0xf0);
        renderer.applyRenderState(state);
        clearInvocations(gl);

        state.setStencil(true,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                TestFunction.Equal, TestFunction.Equal);
        renderer.applyRenderState(state);

        verify(gl).glEnable(GL.GL_STENCIL_TEST);
        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_EQUAL, 2, 0x0f);
        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_EQUAL, 3, 0xf0);
    }

    @Test
    public void invalidatingStateReappliesEnabledStencilParameters() {
        RenderState state = stencil(true, TestFunction.Equal);
        state.setFrontStencilReference(2);
        state.setBackStencilMask(0xf0);
        renderer.applyRenderState(state);
        renderer.invalidateState();
        clearInvocations(gl);

        renderer.applyRenderState(state);

        verify(gl).glEnable(GL.GL_STENCIL_TEST);
        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_EQUAL, 2, Integer.MAX_VALUE);
        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_EQUAL, 0, 0xf0);
    }

    @Test
    public void enablingStencilWithDefaultParametersAppliesBothFaces() {
        renderer.applyRenderState(stencil(true, TestFunction.Always));

        verify(gl).glEnable(GL.GL_STENCIL_TEST);
        verify(gl).glStencilFuncSeparate(GL.GL_FRONT, GL.GL_ALWAYS, 0, Integer.MAX_VALUE);
        verify(gl).glStencilFuncSeparate(GL.GL_BACK, GL.GL_ALWAYS, 0, Integer.MAX_VALUE);
        verify(gl).glStencilOpSeparate(GL.GL_FRONT, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
        verify(gl).glStencilOpSeparate(GL.GL_BACK, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
    }

    private static RenderState stencil(boolean enabled, TestFunction function) {
        RenderState state = new RenderState();
        state.setStencil(enabled,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
                function, function);
        return state;
    }
}
