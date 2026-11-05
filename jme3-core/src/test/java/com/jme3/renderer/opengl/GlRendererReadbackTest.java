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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jme3.renderer.Caps;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


class GlRendererReadbackTest {

    @Test
    void glesFloatReadbackConvertsRgbaWithoutGammaAndPreservesBufferRange() {
        final Readback gl = new Readback(true, new float[] {.25f, .5f, .75f, 1f, -1f, 2f, .1f, 0f});
        ByteBuffer destination = BufferUtils.createByteBuffer(12);
        destination.put(0, (byte) 17).put(11, (byte) 23).position(2).limit(10);

        gl.renderer.readFrameBuffer(framebuffer(Image.Format.RGBA16F, 2), destination);

        assertEquals(GL.GL_RGBA, gl.format);
        assertEquals(GL.GL_FLOAT, gl.type);
        assertEquals(2, destination.position());
        assertEquals(10, destination.limit());
        assertArrayEquals(new int[] {64, 128, 191, 255, 0, 255, 26, 0}, bytes(destination, 2, 8));
        assertEquals(17, destination.get(0));
        destination.limit(destination.capacity());
        assertEquals(23, destination.get(11));
    }

    @Test
    void nullFramebufferUsesFloatingPointMainOverride() {
        final Readback gl = new Readback(true, new float[] {1f, 0f, .5f, 1f});
        gl.renderer.setMainFrameBufferOverride(framebuffer(Image.Format.RGBA16F));
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBuffer(null, destination);

        assertEquals(GL.GL_FLOAT, gl.type);
        assertArrayEquals(new int[] {255, 0, 128, 255}, bytes(destination, 0, 4));
    }

    @Test
    void glesFloatTypedBgraReadbackKeepsRequestedChannelOrder() throws Exception {
        final Readback gl = new Readback(true, new float[] {.25f, .5f, .75f, 1f});
        gl.initializeTextureFormats(EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30, Caps.CoreProfile));
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBufferWithFormat(framebuffer(Image.Format.RGBA32F), destination,
                Image.Format.BGRA8);

        assertEquals(GL.GL_RGBA, gl.format);
        assertEquals(GL.GL_FLOAT, gl.type);
        assertArrayEquals(new int[] {191, 128, 64, 255}, bytes(destination, 0, 4));
    }

    @Test
    void explicitRgbaByteOutputUsesFloatReadback() throws Exception {
        final Readback gl = new Readback(true, new float[] {.25f, .5f, .75f, 1f});
        gl.initializeTextureFormats(EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30, Caps.CoreProfile));
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBufferWithFormat(framebuffer(Image.Format.RGBA16F), destination,
                Image.Format.RGBA8);

        assertEquals(GL.GL_RGBA, gl.format);
        assertEquals(GL.GL_FLOAT, gl.type);
        assertArrayEquals(new int[] {64, 128, 191, 255}, bytes(destination, 0, 4));
    }

    @Test
    void glesNormalizedFramebufferRetainsDirectByteReadback() {
        final Readback gl = new Readback(true, new float[4]);
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBuffer(framebuffer(Image.Format.RGBA8), destination);

        assertEquals(GL.GL_UNSIGNED_BYTE, gl.type);
        assertSame(destination, gl.destination);
    }

    @ParameterizedTest
    @EnumSource(value = Image.Format.class, names = {"RGBA8", "RGBA16F"})
    void replacedTextureImageDeterminesReadbackType(Image.Format originalFormat) {
        final Readback gl = new Readback(true, new float[] {.25f, .5f, .75f, 1f});
        Texture2D texture = new Texture2D(1, 1, originalFormat);
        FrameBuffer fb = new FrameBuffer(1, 1, 1);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
        fb.setId(1);
        fb.clearUpdateNeeded();
        Image.Format currentFormat = originalFormat == Image.Format.RGBA8
                ? Image.Format.RGBA16F : Image.Format.RGBA8;
        texture.setImage(new Image(currentFormat, 1, 1, (ByteBuffer) null, ColorSpace.Linear));
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBuffer(fb, destination);

        if (currentFormat.isFloatingPont()) {
            assertEquals(GL.GL_FLOAT, gl.type);
            assertArrayEquals(new int[] {64, 128, 191, 255}, bytes(destination, 0, 4));
        } else {
            assertEquals(GL.GL_UNSIGNED_BYTE, gl.type);
            assertSame(destination, gl.destination);
        }
    }

    @Test
    void desktopFloatingFramebufferRetainsDirectByteReadback() throws Exception {
        final Readback gl = new Readback(false, new float[4]);
        gl.initializeTextureFormats(EnumSet.of(Caps.OpenGL20));
        ByteBuffer destination = BufferUtils.createByteBuffer(4);

        gl.renderer.readFrameBufferWithFormat(framebuffer(Image.Format.RGBA16F), destination,
                Image.Format.BGRA8);

        assertEquals(GL2.GL_BGRA, gl.format);
        assertEquals(GL.GL_UNSIGNED_BYTE, gl.type);
        assertSame(destination, gl.destination);
    }

    @Test
    void selectedColorAttachmentDeterminesReadbackType() {
        final Readback gl = new Readback(true, new float[] {1f, 0f, .5f, 1f});
        FrameBuffer fb = new FrameBuffer(1, 1, 1);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA16F));
        fb.setTargetIndex(1);
        fb.setId(1);
        fb.clearUpdateNeeded();

        gl.renderer.readFrameBuffer(fb, BufferUtils.createByteBuffer(4));

        assertEquals(GL.GL_FLOAT, gl.type);
    }

    @Test
    void mrtUsesActualReadAttachmentInsteadOfFirstDrawAttachment() {
        final Readback gl = new Readback(true, new float[4]);
        FrameBuffer fb = new FrameBuffer(1, 1, 1);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA16F));
        fb.setMultiTarget(true);
        fb.setId(1);
        fb.clearUpdateNeeded();
        gl.readAttachment = 1;

        gl.renderer.readFrameBuffer(fb, BufferUtils.createByteBuffer(4));

        assertEquals(GL.GL_FLOAT, gl.type);
        gl.readAttachment = 0;
        gl.renderer.readFrameBuffer(fb, BufferUtils.createByteBuffer(4));
        assertEquals(GL.GL_UNSIGNED_BYTE, gl.type);
    }

    @Test
    void explicitFloatOutputRetainsDirectReadback() throws Exception {
        final Readback gl = new Readback(true, new float[] {.25f, .5f, .75f, 1f});
        gl.initializeTextureFormats(EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30, Caps.CoreProfile,
                Caps.FloatTexture, Caps.FloatColorBufferRGBA));
        ByteBuffer destination = BufferUtils.createByteBuffer(16);

        gl.renderer.readFrameBufferWithFormat(framebuffer(Image.Format.RGBA16F), destination,
                Image.Format.RGBA32F);

        assertEquals(GL.GL_FLOAT, gl.type);
        assertSame(destination, gl.destination);
        assertEquals(.75f, destination.asFloatBuffer().get(2));
    }

    @Test
    void rejectsShortOrReadOnlyConversionDestinationBeforeGlRead() {
        final Readback gl = new Readback(true, new float[4]);
        FrameBuffer framebuffer = framebuffer(Image.Format.RGBA16F);

        assertThrows(IllegalArgumentException.class,
                () -> gl.renderer.readFrameBuffer(framebuffer, BufferUtils.createByteBuffer(3)));
        assertThrows(IllegalArgumentException.class,
                () -> gl.renderer.readFrameBuffer(framebuffer,
                        BufferUtils.createByteBuffer(4).asReadOnlyBuffer()));
        assertNull(gl.destination);
    }

    private static FrameBuffer framebuffer(Image.Format format) {
        return framebuffer(format, 1);
    }

    private static FrameBuffer framebuffer(Image.Format format, int height) {
        FrameBuffer fb = new FrameBuffer(1, height, 1);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(format));
        fb.setId(1);
        fb.clearUpdateNeeded();
        return fb;
    }

    private static int[] bytes(ByteBuffer buffer, int offset, int count) {
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = Byte.toUnsignedInt(buffer.get(offset + i));
        }
        return values;
    }

    private static final class Readback {
        final GLRenderer renderer;
        int format;
        int type;
        int readAttachment;
        ByteBuffer destination;
        final float[] pixels;

        Readback(boolean gles, float[] pixels) {
            this.pixels = pixels;
            Class<?> api = gles ? GLES_30.class : GL.class;
            GL gl = (GL) Proxy.newProxyInstance(api.getClassLoader(),
                    new Class<?>[] {api}, (proxy, method, args) -> {
                    if (method.getName().equals("glGetInteger") && (Integer) args[0] == GL2.GL_READ_BUFFER) {
                        ((IntBuffer) args[1]).put(0, GLFbo.GL_COLOR_ATTACHMENT0_EXT + readAttachment);
                    }
                    if (method.getName().equals("glReadPixels")) {
                        format = (Integer) args[4];
                        type = (Integer) args[5];
                        destination = (ByteBuffer) args[6];
                        if (type == GL.GL_FLOAT) {
                            FloatBuffer floats = destination.asFloatBuffer();
                            for (int i = 0; i < pixels.length; i++) {
                                floats.put(i, pixels[i]);
                            }
                        }
                    }
                    return null;
                });
            GLFbo fbo = (GLFbo) Proxy.newProxyInstance(GLFbo.class.getClassLoader(),
                    new Class<?>[] {GLFbo.class},
                    (proxy, method, args) -> null);
            renderer = new GLRenderer(gl, null, fbo);
            renderer.getCaps().add(Caps.FrameBuffer);
            // Exercise a 1xN read region, including nonzero destination positions.
            renderer.setViewPort(0, 0, 1, pixels.length / 4);
        }

        void initializeTextureFormats(EnumSet<Caps> caps) throws Exception {
            Field field = GLRenderer.class.getDeclaredField("texUtil");
            field.setAccessible(true);
            ((TextureUtil) field.get(renderer)).initialize(caps);
        }
    }
}
