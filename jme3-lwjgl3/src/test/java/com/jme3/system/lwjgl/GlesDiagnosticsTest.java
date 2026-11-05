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
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.jme3.renderer.lwjgl.LwjglGLES;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GLES_30;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.NativeLibraryLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.Configuration;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class GlesDiagnosticsTest {

    private static String previousGlesLibrary;

    @BeforeAll
    static void selectBundledGlesLibrary() {
        previousGlesLibrary = Configuration.OPENGLES_LIBRARY_NAME.get();
        // Mocking GLES initializes its loader; use ANGLE on every supported platform.
        NativeLibraryLoader.loadNativeLibrary("angleEGL", true);
        Configuration.OPENGLES_LIBRARY_NAME.set(
                NativeLibraryLoader.loadNativeLibrary("angleGLESv2", true));
    }

    @AfterAll
    static void restoreGlesLibrarySelection() {
        Configuration.OPENGLES_LIBRARY_NAME.set(previousGlesLibrary);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void diagnosticCombinationsInitializeAndForwardThroughRequiredInterfaces(int flags) {
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.ANGLE_GLES3);
        settings.setGraphicsDebug((flags & 1) != 0);
        settings.setGraphicsTiming((flags & 2) != 0);
        settings.setGraphicsTrace((flags & 4) != 0);
        LwjglContext context = mock(LwjglContext.class,
                withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        context.setSettings(settings);
        List<Object> interfaces = new ArrayList<>();

        // Exercise context initialization without requiring a native display or GL context.
        try (MockedStatic<GLES> capabilities = mockStatic(GLES.class);
                MockedConstruction<LwjglGLES> backends = mockConstruction(LwjglGLES.class);
                MockedConstruction<GLRenderer> renderers = mockConstruction(GLRenderer.class,
                        (renderer, construction) -> interfaces.addAll(construction.arguments()))) {
            context.initContextFirstTime();

            GL gl = assertInstanceOf(GL.class, interfaces.get(0));
            GL2 gl2 = assertInstanceOf(GL2.class, gl);
            GLES_30 gles = assertInstanceOf(GLES_30.class, gl);
            GLFbo fbo = assertInstanceOf(GLFbo.class, interfaces.get(2));
            assertInstanceOf(GLExt.class, interfaces.get(1));
            assertSame(gl, interfaces.get(1));
            assertSame(gl, fbo);
            assertSame(renderers.constructed().get(0), context.getRenderer());
            assertTrue(context.isRenderable());

            gl.glViewport(1, 2, 3, 4);
            gl2.glReadBuffer(GLFbo.GL_COLOR_ATTACHMENT0_EXT);
            gles.glBindVertexArray(5);
            fbo.glBindFramebufferEXT(GLFbo.GL_FRAMEBUFFER_EXT, 6);

            LwjglGLES backend = backends.constructed().get(0);
            verify(backend).glViewport(1, 2, 3, 4);
            verify(backend).glReadBuffer(GLFbo.GL_COLOR_ATTACHMENT0_EXT);
            verify(backend).glBindVertexArray(5);
            verify(backend).glBindFramebufferEXT(GLFbo.GL_FRAMEBUFFER_EXT, 6);
        }
    }
}
