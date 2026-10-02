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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendEquation;
import com.jme3.material.RenderState.BlendEquationAlpha;
import com.jme3.material.RenderState.BlendFunc;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.RenderState.StencilOperation;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderContext;
import com.jme3.scene.Mesh;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Models native GL state independently of the renderer's cached RenderContext.
 */
public class GlRendererStateInvalidationTest {

    enum Surface {
        DESKTOP(true), DESKTOP_ADAPTER(true), GLES2(false), GLES3(false),
        GLES_ADAPTER(false), WEBGL_ADAPTER(false);

        final boolean desktop;

        Surface(boolean desktop) {
            this.desktop = desktop;
        }
    }

    @ParameterizedTest
    @EnumSource(Surface.class)
    public void invalidationRestoresDisabledAndDefaultDrawState(Surface surface) {
        RecordingGl recording = new RecordingGl(surface);
        RenderState state = disabledState();
        recording.renderer.applyRenderState(state);
        recording.changeNativeState();
        recording.renderer.invalidateState();
        recording.calls.clear();

        recording.renderer.applyRenderState(state);

        assertAll(
                () -> assertFalse(recording.enabled(GL.GL_DEPTH_TEST), "depth test"),
                () -> recording.assertState("glDepthMask", true),
                () -> recording.assertState("glColorMask", true, true, true, true),
                () -> assertFalse(recording.enabled(GL.GL_CULL_FACE), "face culling"),
                () -> assertFalse(recording.enabled(GL.GL_BLEND), "blending"),
                () -> assertFalse(recording.enabled(GL.GL_STENCIL_TEST), "stencil test"),
                () -> assertFalse(recording.enabled(GL.GL_POLYGON_OFFSET_FILL), "polygon offset"),
                () -> recording.assertState("glLineWidth", 1f));
        if (surface.desktop) {
            recording.assertState("glPolygonMode", GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
        } else {
            assertFalse(recording.calls.contains("glPolygonMode"));
        }
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @ParameterizedTest
    @EnumSource(Surface.class)
    public void disabledDepthDoesNotValidateAnUnknownComparisonFunction(Surface surface) {
        RecordingGl recording = new RecordingGl(surface);
        RenderState state = disabledState();
        recording.renderer.applyRenderState(state);
        recording.gl.glDepthFunc(GL.GL_NEVER);
        recording.renderer.invalidateState();
        recording.renderer.applyRenderState(state);

        state.setDepthTest(true);
        recording.renderer.applyRenderState(state);

        assertTrue(recording.enabled(GL.GL_DEPTH_TEST));
        recording.assertState("glDepthFunc", GL.GL_LESS);
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @ParameterizedTest
    @EnumSource(BlendMode.class)
    public void disabledBlendDoesNotValidateUnknownFactors(BlendMode mode) {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        recording.renderer.applyRenderState(state);
        recording.gl.glBlendFunc(GL.GL_ZERO, GL.GL_ZERO);
        recording.gl.glBlendEquationSeparate(GL2.GL_FUNC_SUBTRACT, GL2.GL_FUNC_REVERSE_SUBTRACT);
        recording.gl.glEnable(GL.GL_BLEND);
        recording.renderer.invalidateState();
        recording.renderer.applyRenderState(state);

        state.setBlendMode(mode);
        recording.renderer.applyRenderState(state);

        assertEquals(mode != BlendMode.Off, recording.enabled(GL.GL_BLEND));
        recording.assertState("glBlendEquationSeparate", GL2.GL_FUNC_ADD, GL2.GL_FUNC_ADD);
        if (mode != BlendMode.Off) {
            int[] factors = expectedFactors(mode);
            recording.assertState("glBlendFuncSeparate", factors[0], factors[1], factors[2], factors[3]);
        }
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @Test
    public void customBlendRestoresFactorsEqualToTheNativeDefaultsAfterADisabledApply() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        state.setCustomBlendFactors(BlendFunc.One, BlendFunc.Zero, BlendFunc.One, BlendFunc.Zero);
        recording.gl.glBlendFunc(GL.GL_ZERO, GL.GL_ONE);
        recording.renderer.invalidateState();
        recording.renderer.applyRenderState(state);

        state.setBlendMode(BlendMode.Custom);
        recording.renderer.applyRenderState(state);

        recording.assertState("glBlendFuncSeparate", GL.GL_ONE, GL.GL_ZERO, GL.GL_ONE, GL.GL_ZERO);
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @ParameterizedTest
    @EnumSource(Surface.class)
    public void invalidationRestoresEnabledNondefaultDrawState(Surface surface) {
        RecordingGl recording = new RecordingGl(surface);
        RenderState state = enabledState();
        recording.renderer.applyRenderState(state);
        recording.restoreNativeDefaults();
        recording.renderer.invalidateState();
        recording.calls.clear();

        recording.renderer.applyRenderState(state);

        assertAll(
                () -> assertTrue(recording.enabled(GL.GL_DEPTH_TEST), "depth test"),
                () -> recording.assertState("glDepthFunc", GL.GL_GREATER),
                () -> recording.assertState("glDepthMask", false),
                () -> recording.assertState("glColorMask", false, false, false, false),
                () -> assertTrue(recording.enabled(GL.GL_CULL_FACE), "face culling"),
                () -> recording.assertState("glCullFace", GL.GL_FRONT),
                () -> assertTrue(recording.enabled(GL.GL_BLEND), "blending"),
                () -> recording.assertState("glBlendEquationSeparate", GL2.GL_FUNC_SUBTRACT,
                        GL2.GL_FUNC_REVERSE_SUBTRACT),
                () -> recording.assertState("glBlendFuncSeparate", GL.GL_SRC_ALPHA,
                        GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ZERO),
                () -> assertTrue(recording.enabled(GL.GL_STENCIL_TEST), "stencil test"),
                () -> recording.assertState("glStencilFuncSeparate:" + GL.GL_FRONT,
                        GL.GL_FRONT, GL.GL_EQUAL, 2, 0x0f),
                () -> recording.assertState("glStencilFuncSeparate:" + GL.GL_BACK,
                        GL.GL_BACK, GL.GL_NOTEQUAL, 3, 0xf0),
                () -> recording.assertState("glStencilOpSeparate:" + GL.GL_FRONT,
                        GL.GL_FRONT, GL.GL_REPLACE, GL.GL_INCR, GL.GL_DECR),
                () -> recording.assertState("glStencilOpSeparate:" + GL.GL_BACK,
                        GL.GL_BACK, GL.GL_ZERO, GL.GL_INVERT, GL.GL_KEEP),
                () -> assertTrue(recording.enabled(GL.GL_POLYGON_OFFSET_FILL), "polygon offset"),
                () -> recording.assertState("glPolygonOffset", 2f, 3f),
                () -> recording.assertState("glLineWidth", 2f));
        if (surface.desktop) {
            recording.assertState("glPolygonMode", GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
        } else {
            assertFalse(recording.calls.contains("glPolygonMode"));
        }
    }

    @ParameterizedTest
    @EnumSource(Surface.class)
    public void freshDefaultsAndUnchangedApplicationsNeedNoGlCalls(Surface surface) {
        RecordingGl recording = new RecordingGl(surface);
        RenderState state = disabledState();

        recording.renderer.applyRenderState(state);
        recording.renderer.applyRenderState(state);

        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @ParameterizedTest
    @EnumSource(BlendMode.class)
    public void freshBlendModesUseNativeFactorDefaults(BlendMode mode) {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        state.setBlendMode(mode);

        recording.renderer.applyRenderState(state);

        assertEquals(mode != BlendMode.Off, recording.enabled(GL.GL_BLEND));
        if (mode == BlendMode.Off) {
            recording.assertState("glBlendFuncSeparate", GL.GL_ONE, GL.GL_ZERO, GL.GL_ONE, GL.GL_ZERO);
        } else {
            int[] factors = expectedFactors(mode);
            recording.assertState("glBlendFuncSeparate", factors[0], factors[1], factors[2], factors[3]);
        }
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertTrue(recording.calls.isEmpty(), recording.calls.toString());
    }

    @ParameterizedTest
    @EnumSource(Surface.class)
    public void invalidationQueriesOnlySupportedDesktopBufferState(Surface surface) {
        RecordingGl recording = new RecordingGl(surface);

        recording.renderer.invalidateState();

        if (surface.desktop) {
            assertEquals(Arrays.asList(GL2.GL_DRAW_BUFFER, GL2.GL_READ_BUFFER), recording.queries);
        } else {
            assertTrue(recording.queries.isEmpty(), recording.queries.toString());
        }
    }

    @Test
    public void uninitializedGlesAdapterDoesNotUseDesktopState() {
        RecordingGl recording = new RecordingGl(Surface.GLES_ADAPTER);
        recording.renderer.getCaps().clear();
        RenderState state = disabledState();
        state.setWireframe(true);

        recording.renderer.invalidateState();
        recording.renderer.applyRenderState(state);

        assertTrue(recording.queries.isEmpty());
        assertFalse(recording.calls.contains("glPolygonMode"));
    }

    @Test
    public void clearingOneBufferDoesNotValidateTheOtherWriteMask() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        recording.gl.glDepthMask(false);
        recording.gl.glColorMask(false, false, false, false);
        recording.renderer.invalidateState();

        recording.renderer.clearBuffers(true, false, false);

        recording.assertState("glColorMask", true, true, true, true);
        recording.assertState("glDepthMask", false);
        recording.renderer.clearBuffers(false, true, false);
        recording.assertState("glDepthMask", true);
        recording.calls.clear();
        recording.renderer.clearBuffers(false, false, false);
        assertTrue(recording.calls.isEmpty());
    }

    @Test
    public void meshLineWidthAndRenderStateShareTheSameCache() {
        final RecordingGl recording = new RecordingGl(Surface.GLES2);
        final Mesh mesh = lineMesh(2f);
        RenderState state = disabledState();
        state.setLineWidth(2f);
        recording.renderer.applyRenderState(state);
        recording.gl.glLineWidth(5f);
        recording.renderer.invalidateState();

        recording.renderer.renderMesh(mesh, 0, 1, null);

        recording.assertState("glLineWidth", 2f);
        recording.calls.clear();
        recording.renderer.applyRenderState(state);
        assertFalse(recording.calls.contains("glLineWidth"));
        recording.calls.clear();
        recording.renderer.renderMesh(mesh, 0, 1, null);
        assertFalse(recording.calls.contains("glLineWidth"));
        state.setLineWidth(1f);
        recording.renderer.applyRenderState(state);
        recording.assertState("glLineWidth", 1f);
    }

    @Test
    public void invalidationRetainsCachedValues() throws ReflectiveOperationException {
        RecordingGl recording = new RecordingGl(Surface.DESKTOP);
        recording.renderer.applyRenderState(enabledState());
        Field contextField = GLRenderer.class.getDeclaredField("context");
        contextField.setAccessible(true);
        RenderContext context = (RenderContext) contextField.get(recording.renderer);

        recording.renderer.invalidateState();

        assertAll(
                () -> assertTrue(context.depthTestEnabled),
                () -> assertEquals(TestFunction.Greater, context.depthFunc),
                () -> assertFalse(context.depthWriteEnabled),
                () -> assertFalse(context.colorWriteEnabled),
                () -> assertEquals(FaceCullMode.Front, context.cullMode),
                () -> assertEquals(BlendMode.Custom, context.blendMode),
                () -> assertEquals(BlendEquation.Subtract, context.blendEquation),
                () -> assertEquals(BlendFunc.Src_Alpha, context.sfactorRGB),
                () -> assertTrue(context.polyOffsetEnabled),
                () -> assertEquals(2f, context.polyOffsetFactor),
                () -> assertEquals(3f, context.polyOffsetUnits),
                () -> assertEquals(TestFunction.Equal, context.frontStencilFunction),
                () -> assertEquals(2f, context.lineWidth),
                () -> assertTrue(context.wireframe));
    }

    @Test
    public void defaultMeshWidthRestoresAnUnknownLineWidthWithoutAMaterialApply() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        recording.gl.glLineWidth(5f);
        recording.renderer.invalidateState();
        recording.calls.clear();
        Mesh mesh = lineMesh(1f);

        recording.renderer.renderMesh(mesh, 0, 1, null);

        recording.assertState("glLineWidth", 1f);
        assertTrue(recording.calls.contains("glLineWidth"));
        recording.calls.clear();
        recording.renderer.renderMesh(mesh, 0, 1, null);
        assertFalse(recording.calls.contains("glLineWidth"));
    }

    @Test
    public void defaultMeshWidthPreservesTheKnownMaterialLineWidth() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        recording.gl.glLineWidth(5f);
        recording.renderer.invalidateState();
        RenderState state = disabledState();
        state.setLineWidth(3f);
        recording.renderer.applyRenderState(state);
        recording.calls.clear();

        recording.renderer.renderMesh(lineMesh(1f), 0, 1, null);

        recording.assertState("glLineWidth", 3f);
        assertFalse(recording.calls.contains("glLineWidth"));
    }

    @Test
    public void clearingAfterInvalidationRestoresOnlyTheRequestedWriteMasks() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        recording.renderer.applyRenderState(disabledState());
        recording.gl.glDepthMask(false);
        recording.gl.glColorMask(false, true, false, true);
        recording.gl.glEnable(GL.GL_DEPTH_TEST);
        recording.renderer.invalidateState();

        recording.renderer.clearBuffers(true, true, false);

        recording.assertState("glDepthMask", true);
        recording.assertState("glColorMask", true, true, true, true);
        recording.assertState("glClear", GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        recording.calls.clear();
        recording.renderer.clearBuffers(true, true, false);
        assertEquals(Arrays.asList("glClear"), recording.calls);
        recording.renderer.applyRenderState(disabledState());
        assertFalse(recording.enabled(GL.GL_DEPTH_TEST));
    }

    @Test
    public void disabledStencilIgnoresItsUnusedParametersAfterInvalidation() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        state.setStencil(false, null, null, null, null, null, null, null, null);
        recording.gl.glEnable(GL.GL_STENCIL_TEST);
        recording.renderer.invalidateState();
        recording.calls.clear();

        recording.renderer.applyRenderState(state);

        assertFalse(recording.enabled(GL.GL_STENCIL_TEST));
        assertFalse(recording.calls.contains("glStencilFuncSeparate"));
        assertFalse(recording.calls.contains("glStencilOpSeparate"));
    }

    @Test
    public void enablingPolygonOffsetAfterDisabledRecoveryRestoresItsParameters() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        recording.gl.glPolygonOffset(9f, 8f);
        recording.renderer.invalidateState();
        recording.renderer.applyRenderState(state);

        state.setPolyOffset(2f, 3f);
        recording.renderer.applyRenderState(state);

        assertTrue(recording.enabled(GL.GL_POLYGON_OFFSET_FILL));
        recording.assertState("glPolygonOffset", 2f, 3f);
    }

    @Test
    public void objectResetAndCleanupInvalidateDrawState() {
        RecordingGl recording = new RecordingGl(Surface.GLES2);
        RenderState state = disabledState();
        recording.renderer.applyRenderState(state);
        recording.gl.glDepthMask(false);
        recording.renderer.resetGLObjects();
        recording.renderer.applyRenderState(state);
        recording.assertState("glDepthMask", true);
        recording.gl.glColorMask(false, false, false, false);
        recording.renderer.cleanup();
        recording.renderer.applyRenderState(state);
        recording.assertState("glColorMask", true, true, true, true);
    }

    private static Mesh lineMesh(float lineWidth) {
        Mesh mesh = new Mesh() {
            @Override
            public int getVertexCount() {
                return 2;
            }

            @Override
            public int getTriangleCount() {
                return 1;
            }
        };
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setLineWidth(lineWidth);
        return mesh;
    }

    private static RenderState disabledState() {
        RenderState state = new RenderState();
        state.setDepthTest(false);
        state.setDepthFunc(TestFunction.Less);
        state.setFaceCullMode(FaceCullMode.Off);
        state.setFrontStencilMask(-1);
        state.setBackStencilMask(-1);
        return state;
    }

    private static RenderState enabledState() {
        RenderState state = disabledState();
        state.setWireframe(true);
        state.setDepthTest(true);
        state.setDepthFunc(TestFunction.Greater);
        state.setDepthWrite(false);
        state.setColorWrite(false);
        state.setFaceCullMode(FaceCullMode.Front);
        state.setBlendMode(BlendMode.Custom);
        state.setBlendEquation(BlendEquation.Subtract);
        state.setBlendEquationAlpha(BlendEquationAlpha.ReverseSubtract);
        state.setCustomBlendFactors(BlendFunc.Src_Alpha, BlendFunc.One_Minus_Src_Alpha,
                BlendFunc.One, BlendFunc.Zero);
        state.setStencil(true, StencilOperation.Replace, StencilOperation.Increment,
                StencilOperation.Decrement,
                StencilOperation.Zero, StencilOperation.Invert, StencilOperation.Keep,
                TestFunction.Equal, TestFunction.NotEqual);
        state.setFrontStencilReference(2);
        state.setBackStencilReference(3);
        state.setFrontStencilMask(0x0f);
        state.setBackStencilMask(0xf0);
        state.setPolyOffset(2f, 3f);
        state.setLineWidth(2f);
        return state;
    }

    private static int[] expectedFactors(BlendMode mode) {
        switch (mode) {
            case Additive:
            case Custom:
                return new int[]{GL.GL_ONE, GL.GL_ONE, GL.GL_ONE, GL.GL_ONE};
            case AlphaAdditive:
                return new int[]{GL.GL_SRC_ALPHA, GL.GL_ONE, GL.GL_SRC_ALPHA, GL.GL_ONE};
            case Alpha:
                return new int[]{GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA,
                    GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA};
            case AlphaSumA:
                return new int[]{GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE};
            case PremultAlpha:
                return new int[]{GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA};
            case Modulate:
                return new int[]{GL.GL_DST_COLOR, GL.GL_ZERO, GL.GL_DST_COLOR, GL.GL_ZERO};
            case ModulateX2:
                return new int[]{GL.GL_DST_COLOR, GL.GL_SRC_COLOR, GL.GL_DST_COLOR, GL.GL_SRC_COLOR};
            case Color:
            case Screen:
                return new int[]{GL.GL_ONE, GL.GL_ONE_MINUS_SRC_COLOR, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_COLOR};
            case Exclusion:
                return new int[]{GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR,
                    GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR};
            default:
                throw new AssertionError(mode);
        }
    }

    private static final class RecordingGl implements InvocationHandler {
        private final Surface surface;
        private final GL gl;
        private final GLRenderer renderer;
        private final Map<String, List<Object>> state = new HashMap<>();
        private final Map<Integer, Boolean> enables = new HashMap<>();
        private final List<String> calls = new ArrayList<>();
        private final List<Integer> queries = new ArrayList<>();

        private RecordingGl(Surface surface) {
            this.surface = surface;
            Class<?> glType = surface.desktop ? GL3.class
                    : surface == Surface.GLES2 ? GL.class : GLES_30.class;
            Class<?> adapterType;
            if (surface == Surface.DESKTOP_ADAPTER) {
                adapterType = GLES_30.class;
            } else if (surface == Surface.GLES_ADAPTER || surface == Surface.WEBGL_ADAPTER) {
                adapterType = GL2.class;
            } else {
                adapterType = GLExt.class;
            }
            gl = (GL) Proxy.newProxyInstance(GL.class.getClassLoader(),
                    new Class<?>[]{glType, adapterType, GLFbo.class}, this);
            GLExt extension = (GLExt) Proxy.newProxyInstance(GLExt.class.getClassLoader(),
                    new Class<?>[]{GLExt.class}, this);
            restoreNativeDefaults();
            renderer = new GLRenderer(gl, extension, (GLFbo) gl);
            if (surface.desktop) {
                renderer.getCaps().add(Caps.OpenGL20);
                renderer.getCaps().add(Caps.OpenGL30);
            } else if (surface == Surface.WEBGL_ADAPTER) {
                renderer.getCaps().add(Caps.WebGL);
            } else {
                renderer.getCaps().add(Caps.OpenGLES20);
                if (surface != Surface.GLES2) {
                    renderer.getCaps().add(Caps.OpenGLES30);
                }
            }
            calls.clear();
        }

        private boolean enabled(int capability) {
            return Boolean.TRUE.equals(enables.get(capability));
        }

        private void assertState(String name, Object... expected) {
            assertEquals(Arrays.asList(expected), state.get(name), name);
        }

        private void restoreNativeDefaults() {
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LESS);
            gl.glDepthMask(true);
            gl.glColorMask(true, true, true, true);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);
            gl.glDisable(GL.GL_BLEND);
            gl.glBlendEquationSeparate(GL2.GL_FUNC_ADD, GL2.GL_FUNC_ADD);
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
            gl.glDisable(GL.GL_STENCIL_TEST);
            gl.glStencilFuncSeparate(GL.GL_FRONT, GL.GL_ALWAYS, 0, -1);
            gl.glStencilFuncSeparate(GL.GL_BACK, GL.GL_ALWAYS, 0, -1);
            gl.glStencilOpSeparate(GL.GL_FRONT, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
            gl.glStencilOpSeparate(GL.GL_BACK, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
            gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(0f, 0f);
            gl.glLineWidth(1f);
            if (surface.desktop) {
                ((GL2) gl).glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
            }
        }

        private void changeNativeState() {
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_NEVER);
            gl.glDepthMask(false);
            gl.glColorMask(false, true, false, true);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_FRONT);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendEquationSeparate(GL2.GL_FUNC_SUBTRACT, GL2.GL_FUNC_REVERSE_SUBTRACT);
            gl.glBlendFunc(GL.GL_ZERO, GL.GL_ZERO);
            gl.glEnable(GL.GL_STENCIL_TEST);
            gl.glStencilFuncSeparate(GL.GL_FRONT, GL.GL_NEVER, 1, 0x0f);
            gl.glStencilFuncSeparate(GL.GL_BACK, GL.GL_NEVER, 2, 0xf0);
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(8f, 9f);
            gl.glLineWidth(3f);
            if (surface.desktop) {
                ((GL2) gl).glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            if (name.equals("glGetInteger")) {
                int parameter = (Integer) arguments[0];
                queries.add(parameter);
                if (!surface.desktop
                        && (parameter == GL2.GL_DRAW_BUFFER || parameter == GL2.GL_READ_BUFFER)) {
                    throw new AssertionError("Unsupported GLES query: " + parameter);
                }
                ((IntBuffer) arguments[1]).put(0, 0);
                return null;
            }
            calls.add(name);
            if (name.equals("glEnable") || name.equals("glDisable")) {
                enables.put((Integer) arguments[0], name.equals("glEnable"));
            } else if (name.equals("glBlendFunc")) {
                state.put("glBlendFuncSeparate", Arrays.asList(arguments[0], arguments[1],
                        arguments[0], arguments[1]));
            } else {
                String key = name.startsWith("glStencil") ? name + ":" + arguments[0] : name;
                state.put(key, Arrays.asList(arguments));
            }
            return null;
        }
    }
}
