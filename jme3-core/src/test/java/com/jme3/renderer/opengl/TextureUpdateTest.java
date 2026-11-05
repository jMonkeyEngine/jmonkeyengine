/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.opengl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression coverage for 2D subimage uploads after texture-unit cache hits. */
class TextureUpdateTest {

    @Test
    @SuppressWarnings("deprecation")
    void bothTwoDimensionalUpdateOverloadsTargetUnitZeroAfterAnotherUnitWasBound() throws Exception {
        RecordingGl gl = new RecordingGl();
        GLRenderer renderer = gl.renderer();
        try {
            Image patch = image(1, 1, (byte) 0x5a);

            Texture2D deprecatedDestination = new Texture2D(image(2, 2, (byte) 0x11));
            assertSubImageTargetsUnitZero(gl, renderer, deprecatedDestination, () ->
                    renderer.modifyTexture((Texture) deprecatedDestination, patch, 0, 0));

            Texture2D rectangleDestination = new Texture2D(image(2, 2, (byte) 0x22));
            assertSubImageTargetsUnitZero(gl, renderer, rectangleDestination, () ->
                    renderer.modifyTexture(rectangleDestination, patch, 1, 1, 0, 0, 1, 1));
        } finally {
            renderer.cleanup();
        }
    }

    private static void assertSubImageTargetsUnitZero(RecordingGl gl, GLRenderer renderer,
            Texture2D destination, Runnable update) throws TextureUnitException {
        Texture2D otherTexture = new Texture2D(image(1, 1, (byte) 0x33));
        renderer.setTexture(0, destination);
        renderer.setTexture(3, otherTexture);
        gl.lastUpload = null;

        update.run();

        assertNotNull(gl.lastUpload, "the update should submit a 2D subimage");
        assertEquals(GL.GL_TEXTURE_2D, gl.lastUpload.target);
        assertEquals(GL.GL_TEXTURE0, gl.lastUpload.activeTexture,
                "the upload must reactivate unit zero while unit three is active");
        assertEquals(destination.getImage().getId(), gl.lastUpload.boundTexture,
                "the upload must still have its destination bound on unit zero");
    }

    private static Image image(int width, int height, byte value) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        while (data.hasRemaining()) {
            data.put(value);
        }
        data.flip();
        return new Image(Image.Format.RGBA8, width, height, data, ColorSpace.Linear);
    }

    private static final class Upload {
        final int target;
        final int activeTexture;
        final int boundTexture;

        Upload(int target, int activeTexture, int boundTexture) {
            this.target = target;
            this.activeTexture = activeTexture;
            this.boundTexture = boundTexture;
        }
    }

    private static final class RecordingGl implements InvocationHandler {
        private final Map<Long, Integer> boundTextures = new HashMap<>();
        private int activeTexture = GL.GL_TEXTURE0;
        private int nextTextureId = 1;
        private Upload lastUpload;

        GLRenderer renderer() {
            GLRenderer renderer = new GLRenderer(proxy(GL.class), proxy(GLExt.class), proxy(GLFbo.class));
            renderer.initialize();
            return renderer;
        }

        private <T> T proxy(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                    new Class<?>[]{type}, this));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            if ("glGetString".equals(name)) {
                int parameter = (Integer) arguments[0];
                if (parameter == GL.GL_VERSION) {
                    return "OpenGL ES 2.0";
                }
                if (parameter == GL.GL_SHADING_LANGUAGE_VERSION) {
                    return "OpenGL ES GLSL ES 1.00";
                }
                if (parameter == GL.GL_EXTENSIONS) {
                    return "";
                }
                return "TextureUpdateTest recording backend";
            }
            if ("glGetInteger".equals(name)) {
                int parameter = (Integer) arguments[0];
                int value = parameter == GL.GL_FRAMEBUFFER_BINDING || parameter == GLExt.GL_SAMPLE_BUFFERS_ARB
                        || parameter == GLExt.GL_SAMPLES_ARB ? 0 : 16;
                ((IntBuffer) arguments[1]).put(0, value);
            } else if ("glGenTextures".equals(name)) {
                ((IntBuffer) arguments[0]).put(0, nextTextureId++);
            } else if ("glActiveTexture".equals(name)) {
                activeTexture = (Integer) arguments[0];
            } else if ("glBindTexture".equals(name)) {
                int target = (Integer) arguments[0];
                int id = (Integer) arguments[1];
                boundTextures.put(bindingKey(activeTexture, target), id);
            } else if ("glTexSubImage2D".equals(name)) {
                int target = (Integer) arguments[0];
                lastUpload = new Upload(target, activeTexture,
                        boundTextures.getOrDefault(bindingKey(activeTexture, target), 0));
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == float.class) {
                return 0f;
            }
            if (returnType == long.class) {
                return 0L;
            }
            return null;
        }

        private static long bindingKey(int unit, int target) {
            return ((long) unit << 32) | (target & 0xffffffffL);
        }
    }
}
