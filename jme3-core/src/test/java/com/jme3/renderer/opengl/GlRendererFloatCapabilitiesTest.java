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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

public class GlRendererFloatCapabilitiesTest {

    @Test
    public void testDesktopGl3HasCoreFloatFilteringAndRenderability() {
        EnumSet<Caps> caps = initialize(GL3.class, "3.0");

        assertTrue(caps.contains(Caps.OpenGL30));
        assertFalse(caps.contains(Caps.OpenGLES30));
        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertTrue(caps.contains(Caps.FloatTexture));
        assertTrue(caps.contains(Caps.HalfFloatTextureFilter));
        assertTrue(caps.contains(Caps.FloatTextureFilter));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.R16F, true, true);
        assertFormat(formats, Image.Format.RG16F, true, true);
        assertFormat(formats, Image.Format.RGB16F, true, true);
        assertFormat(formats, Image.Format.RGBA16F, true, true);
        assertFormat(formats, Image.Format.R32F, true, true);
        assertFormat(formats, Image.Format.RG32F, true, true);
        assertFormat(formats, Image.Format.RGB32F, true, true);
        assertFormat(formats, Image.Format.RGBA32F, true, true);
    }

    @Test
    public void testGles3HalfFloatFilteringDoesNotNeedLinearExtension() {
        EnumSet<Caps> caps = initialize(GLES_30.class, "OpenGL ES 3.0",
                "GL_EXT_color_buffer_float");

        assertTrue(caps.contains(Caps.OpenGLES30));
        assertFalse(caps.contains(Caps.OpenGL30));
        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertTrue(caps.contains(Caps.FloatTexture));
        assertTrue(caps.contains(Caps.HalfFloatTextureFilter));
        assertFalse(caps.contains(Caps.FloatTextureFilter));
        assertTrue(caps.contains(Caps.HalfFloatColorBufferR));
        assertTrue(caps.contains(Caps.HalfFloatColorBufferRG));
        assertTrue(caps.contains(Caps.HalfFloatColorBufferRGBA));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRGB));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.R16F, true, true);
        assertFormat(formats, Image.Format.RG16F, true, true);
        assertFormat(formats, Image.Format.RGB16F, true, false);
        assertFormat(formats, Image.Format.RGBA16F, true, true);
        assertFormat(formats, Image.Format.R32F, false, true);
        assertFormat(formats, Image.Format.RG32F, false, true);
        assertFormat(formats, Image.Format.RGB32F, false, false);
        assertFormat(formats, Image.Format.RGBA32F, false, true);
    }

    @Test
    public void testGles3FloatLinearExtensionEnablesFloatFilteringSeparately() {
        EnumSet<Caps> caps = initialize(GLES_30.class, "OpenGL ES 3.0",
                "GL_EXT_color_buffer_float", "GL_OES_texture_float_linear");

        assertTrue(caps.contains(Caps.HalfFloatTextureFilter));
        assertTrue(caps.contains(Caps.FloatTextureFilter));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.R16F, true, true);
        assertFormat(formats, Image.Format.RGBA16F, true, true);
        assertFormat(formats, Image.Format.R32F, true, true);
        assertFormat(formats, Image.Format.RG32F, true, true);
        assertFormat(formats, Image.Format.RGB32F, true, false);
        assertFormat(formats, Image.Format.RGBA32F, true, true);
    }

    @Test
    public void testGles3HalfFloatFilteringDoesNotImplyColorRenderability() {
        EnumSet<Caps> caps = initialize(GLES_30.class, "OpenGL ES 3.0");

        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertTrue(caps.contains(Caps.HalfFloatTextureFilter));
        assertFalse(caps.contains(Caps.FloatTextureFilter));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferR));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRG));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRGB));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRGBA));
        assertFalse(caps.contains(Caps.FloatColorBuffer));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.R16F, true, false);
        assertFormat(formats, Image.Format.RG16F, true, false);
        assertFormat(formats, Image.Format.RGB16F, true, false);
        assertFormat(formats, Image.Format.RGBA16F, true, false);
        assertFormat(formats, Image.Format.R32F, false, false);
        assertFormat(formats, Image.Format.RGBA32F, false, false);
    }

    @Test
    public void testGles2HalfFloatStorageDoesNotImplyFiltering() {
        EnumSet<Caps> caps = initialize(GL.class, "OpenGL ES 2.0",
                "GL_OES_texture_half_float");

        assertTrue(caps.contains(Caps.OpenGLES20));
        assertFalse(caps.contains(Caps.OpenGLES30));
        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertFalse(caps.contains(Caps.HalfFloatTextureFilter));
        assertFalse(caps.contains(Caps.FloatTexture));
        assertFalse(caps.contains(Caps.FloatTextureFilter));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.RGB16F, false, false);
        assertFormat(formats, Image.Format.RGBA16F, false, false);
    }

    @Test
    public void testGles2HalfFloatLinearExtensionEnablesFiltering() {
        EnumSet<Caps> caps = initialize(GL.class, "OpenGL ES 2.0",
                "GL_OES_texture_half_float", "GL_OES_texture_half_float_linear");

        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertTrue(caps.contains(Caps.HalfFloatTextureFilter));
        assertFalse(caps.contains(Caps.FloatTextureFilter));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRGBA));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.RGB16F, true, false);
        assertFormat(formats, Image.Format.RGBA16F, true, false);
    }

    @Test
    public void testGles2HalfFloatRenderabilityDoesNotImplyFiltering() {
        EnumSet<Caps> caps = initialize(GL.class, "OpenGL ES 2.0",
                "GL_OES_texture_half_float", "GL_EXT_color_buffer_half_float");

        assertTrue(caps.contains(Caps.HalfFloatTexture));
        assertFalse(caps.contains(Caps.HalfFloatTextureFilter));
        assertTrue(caps.contains(Caps.HalfFloatColorBufferRGBA));
        assertFalse(caps.contains(Caps.HalfFloatColorBufferRGB));
        assertFalse(caps.contains(Caps.FloatColorBuffer));

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        assertFormat(formats, Image.Format.RGB16F, false, false);
        assertFormat(formats, Image.Format.RGBA16F, false, true);
    }

    @Test
    public void testGles2HalfFloatLinearExtensionStillRequiresStorage() {
        EnumSet<Caps> caps = initialize(GL.class, "OpenGL ES 2.0",
                "GL_OES_texture_half_float_linear");

        assertFalse(caps.contains(Caps.HalfFloatTexture));
        assertFalse(caps.contains(Caps.HalfFloatTextureFilter));
        assertFalse(caps.contains(Caps.FloatTextureFilter));
        assertNull(GLImageFormats.getFormatsForCaps(caps)[0][Image.Format.RGBA16F.ordinal()]);
    }

    private static void assertFormat(GLImageFormat[][] formats, Image.Format format,
            boolean filterable, boolean colorRenderable) {
        GLImageFormat glFormat = formats[0][format.ordinal()];
        assertNotNull(glFormat, format + " should support texture storage");
        assertEquals(filterable, glFormat.filterable, format + " filtering");
        assertEquals(colorRenderable, glFormat.colorRenderable, format + " color renderability");
    }

    private static EnumSet<Caps> initialize(Class<? extends GL> glInterface,
            String version, String... extensions) {
        TestGl driver = new TestGl(version, extensions);
        GLRenderer renderer = new GLRenderer(driver.createProxy(glInterface),
                driver.createProxy(GLExt.class), driver.createProxy(GLFbo.class));
        // Exercise the public initialization path, including extension discovery,
        // rather than pre-populating the capabilities under test.
        renderer.initialize();
        assertEquals(glInterface == GL.class ? 1 : 0, driver.legacyExtensionQueries);
        assertEquals(glInterface == GL.class ? 0 : extensions.length,
                driver.indexedExtensionQueries);
        return renderer.getCaps();
    }

    /** Minimal driver responses needed to initialize without a native GL context. */
    private static final class TestGl implements InvocationHandler {
        private final String version;
        private final String[] extensions;
        private int legacyExtensionQueries;
        private int indexedExtensionQueries;

        private TestGl(String version, String[] extensions) {
            this.version = version;
            this.extensions = extensions;
        }

        private <T> T createProxy(Class<T> glInterface) {
            return glInterface.cast(Proxy.newProxyInstance(glInterface.getClassLoader(),
                    new Class<?>[] {glInterface}, this));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "glGetString":
                    int name = (Integer) arguments[0];
                    if (name == GL.GL_EXTENSIONS) {
                        if (arguments.length == 2) {
                            indexedExtensionQueries++;
                            return extensions[(Integer) arguments[1]];
                        }
                        legacyExtensionQueries++;
                        return String.join(" ", extensions);
                    }
                    if (name == GL.GL_VERSION) {
                        return version;
                    }
                    if (name == GL.GL_SHADING_LANGUAGE_VERSION) {
                        return "1.30";
                    }
                    if (name == GL.GL_VENDOR || name == GL.GL_RENDERER) {
                        return "Test GL driver";
                    }
                    throw new AssertionError("Unexpected string query: " + name);
                case "glGetInteger":
                    int parameter = (Integer) arguments[0];
                    int value;
                    if (parameter == GL3.GL_NUM_EXTENSIONS) {
                        value = extensions.length;
                    } else if (parameter == GL.GL_FRAMEBUFFER_BINDING
                            || parameter == GLExt.GL_SAMPLE_BUFFERS_ARB
                            || parameter == GLExt.GL_SAMPLES_ARB) {
                        value = 0;
                    } else {
                        value = 16;
                    }
                    ((IntBuffer) arguments[1]).put(0, value);
                    return null;
                case "glGenVertexArrays":
                    ((IntBuffer) arguments[0]).put(0, 1);
                    return null;
                case "glIsEnabled":
                case "supportsGpuTimerQuery":
                    return false;
                default:
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    throw new AssertionError("Unexpected GL call: " + method);
            }
        }
    }
}
