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

import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests cubemap parameter targets separately from face image upload targets. */
public class TextureUtilCubemapSwizzleTest {

    @ParameterizedTest
    @MethodSource("swizzledFormats")
    public void testCubemapSwizzleUsesWholeTextureTarget(boolean gles, Image.Format format) {
        checkUpload(gles, format, Texture.Type.CubeMap);
    }

    @ParameterizedTest
    @MethodSource("swizzledFormats")
    public void testTwoDimensionalSwizzleKeepsItsTarget(boolean gles, Image.Format format) {
        checkUpload(gles, format, Texture.Type.TwoDimensional);
    }

    private static Stream<Arguments> swizzledFormats() {
        return Stream.of(
                Arguments.of(false, Image.Format.Alpha8),
                Arguments.of(false, Image.Format.Luminance8),
                Arguments.of(false, Image.Format.Luminance8Alpha8),
                Arguments.of(true, Image.Format.BGR8),
                Arguments.of(true, Image.Format.ARGB8),
                Arguments.of(true, Image.Format.BGRA8),
                Arguments.of(true, Image.Format.ABGR8));
    }

    private static void checkUpload(boolean gles, Image.Format format, Texture.Type type) {
        RecordingGl driver = new RecordingGl(gles);
        GLRenderer renderer = driver.renderer();
        assertTrue(renderer.getCaps().contains(Caps.CoreProfile));
        final int faceCount = type == Texture.Type.CubeMap ? 6 : 1;
        int bytesPerPixel = format.getBitsPerPixel() / 8;
        final int[] mipSizes = {16 * bytesPerPixel, 4 * bytesPerPixel, bytesPerPixel};
        ArrayList<ByteBuffer> data = new ArrayList<>();
        for (int face = 0; face < faceCount; face++) {
            data.add(ByteBuffer.allocateDirect(21 * bytesPerPixel));
        }
        Image image = new Image(format, 4, 4, 0, data, mipSizes, ColorSpace.Linear);

        renderer.updateTexImageData(image, type, 0, false);

        assertEquals(faceCount * mipSizes.length, driver.uploads.size());
        for (int face = 0; face < faceCount; face++) {
            for (int level = 0; level < mipSizes.length; level++) {
                Upload upload = driver.uploads.get(face * mipSizes.length + level);
                int expectedImageTarget = type == Texture.Type.CubeMap
                        ? GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face : GL.GL_TEXTURE_2D;
                assertEquals(expectedImageTarget, upload.target,
                        "Image definitions still address individual faces");
                assertEquals(level, upload.level);
                assertEquals(4 >> level, upload.width);
                assertEquals(4 >> level, upload.height);
                assertEquals(mipSizes[level], upload.bytes);
            }
        }
        assertFalse(driver.swizzleTargets.isEmpty(), "The tested format must require channel swizzles");
        int expectedParameterTarget = type == Texture.Type.CubeMap
                ? GL.GL_TEXTURE_CUBE_MAP : GL.GL_TEXTURE_2D;
        for (int target : driver.swizzleTargets) {
            assertEquals(expectedParameterTarget, target,
                    "Swizzles require the texture target, never a face target");
        }
    }

    private static class Upload {
        final int target;
        final int level;
        final int width;
        final int height;
        final int bytes;

        Upload(Object[] arguments) {
            target = (Integer) arguments[0];
            level = (Integer) arguments[1];
            width = (Integer) arguments[3];
            height = (Integer) arguments[4];
            bytes = ((ByteBuffer) arguments[8]).remaining();
        }
    }

    private static class RecordingGl implements InvocationHandler {
        private final boolean gles;
        private final List<Integer> swizzleTargets = new ArrayList<>();
        private final List<Upload> uploads = new ArrayList<>();
        private int nextId = 1;

        RecordingGl(boolean gles) {
            this.gles = gles;
        }

        private <T> T proxy(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, this));
        }

        GLRenderer renderer() {
            GL api = gles ? proxy(GLES_30.class) : proxy(GL3.class);
            GLRenderer renderer = new GLRenderer(api, proxy(GLExt.class), proxy(GLFbo.class));
            renderer.initialize();
            return renderer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "glGetString":
                    int name = (Integer) arguments[0];
                    if (name == GL.GL_VERSION) {
                        return gles ? "OpenGL ES 3.0" : "3.3";
                    }
                    if (name == GL.GL_SHADING_LANGUAGE_VERSION) {
                        return gles ? "OpenGL ES GLSL ES 3.00" : "3.30";
                    }
                    if (name == GL.GL_VENDOR || name == GL.GL_RENDERER) {
                        return "Recording GL";
                    }
                    if (name == GL.GL_EXTENSIONS) {
                        return "";
                    }
                    throw new AssertionError("Unexpected string query: " + name);
                case "glGetInteger":
                    int parameter = (Integer) arguments[0];
                    int value = parameter == GL3.GL_NUM_EXTENSIONS
                            || parameter == GL.GL_FRAMEBUFFER_BINDING
                            || parameter == GLExt.GL_SAMPLE_BUFFERS_ARB
                            || parameter == GLExt.GL_SAMPLES_ARB ? 0 : 16;
                    ((IntBuffer) arguments[1]).put(0, value);
                    return null;
                case "glGenTextures":
                case "glGenVertexArrays":
                    ((IntBuffer) arguments[0]).put(0, nextId++);
                    return null;
                case "glTexParameteri":
                    int pname = (Integer) arguments[1];
                    if (pname == GL3.GL_TEXTURE_SWIZZLE_R || pname == GL3.GL_TEXTURE_SWIZZLE_G
                            || pname == GL3.GL_TEXTURE_SWIZZLE_B || pname == GL3.GL_TEXTURE_SWIZZLE_A) {
                        swizzleTargets.add((Integer) arguments[0]);
                    }
                    return null;
                case "glTexImage2D":
                    uploads.add(new Upload(arguments));
                    return null;
                case "glIsEnabled":
                case "supportsGpuTimerQuery":
                    return false;
                default:
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    throw new AssertionError("Unstubbed GL call, extend RecordingGl: " + method);
            }
        }
    }
}
