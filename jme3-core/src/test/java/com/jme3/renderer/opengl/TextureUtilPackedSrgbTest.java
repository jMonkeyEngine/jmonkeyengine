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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies packed sRGB transfers without creating a native GL context. */
public class TextureUtilPackedSrgbTest {

    private static final Format[] PACKED_FORMATS = {Format.RGB565, Format.RGB5A1};

    @Test
    public void testGles3SrgbFormatCapabilities() {
        TextureUtil util = new RecordingGl().textureUtil(true, true);
        GLImageFormat rgb = util.getImageFormat(Format.RGB565, true);
        GLImageFormat rgba = util.getImageFormat(Format.RGB5A1, true);
        assertEquals(GLExt.GL_SRGB8_EXT, rgb.internalFormat);
        assertEquals(GLExt.GL_SRGB8_ALPHA8_EXT, rgba.internalFormat);
        assertEquals(GL.GL_RGB, rgb.format);
        assertEquals(GL.GL_RGBA, rgba.format);
        assertEquals(GL.GL_UNSIGNED_BYTE, rgb.dataType);
        assertEquals(GL.GL_UNSIGNED_BYTE, rgba.dataType);
        assertFalse(rgb.colorRenderable);
        assertTrue(rgba.colorRenderable);
        assertTrue(rgb.filterable);
        assertTrue(rgba.filterable);
    }

    @Test
    public void testRendererUploadsEncodedRgb565Channels() {
        RecordingGl gl = new RecordingGl();
        final GLRenderer renderer = gl.renderer();
        ByteBuffer data = packed(0x0000, 0xffff, 0xf800, 0x07e0, 0x001f, 0x1863, 0x8410, 0x39e7);
        Image image = image(Format.RGB565, 4, 2, data);
        renderer.updateTexImageData(image, Texture.Type.TwoDimensional, 0, false);
        Upload upload = gl.uploads.get(0);
        assertSrgb(upload, Format.RGB565);
        assertArrayEquals(bytes(0, 0, 0, 255, 255, 255, 255, 0, 0, 0, 255, 0,
                0, 0, 255, 25, 12, 25, 132, 130, 132, 58, 61, 58), upload.data);
        assertTrue(upload.direct);
    }

    @Test
    public void testRendererUploadsEncodedRgb5a1ChannelsAndAlpha() {
        RecordingGl gl = new RecordingGl();
        final GLRenderer renderer = gl.renderer();
        ByteBuffer data = packed(0x0000, 0xffff, 0xf801, 0x07c0, 0x003f, 0x18c6, 0x8421, 0x39ce);
        renderer.updateTexImageData(image(Format.RGB5A1, 4, 2, data),
                Texture.Type.TwoDimensional, 0, false);
        Upload upload = gl.uploads.get(0);
        assertSrgb(upload, Format.RGB5A1);
        assertArrayEquals(bytes(0, 0, 0, 0, 255, 255, 255, 255, 255, 0, 0, 255,
                0, 255, 0, 0, 0, 0, 255, 255, 25, 25, 25, 0,
                132, 132, 132, 255, 58, 58, 58, 0), upload.data);
    }

    @Test
    public void testAllPackedValuesUseNormalizedEncodedChannels() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = ByteBuffer.allocateDirect(65536 * 2).order(ByteOrder.nativeOrder());
            byte[] expected = new byte[65536 * components(format)];
            int offset = 0;
            for (int value = 0; value <= 0xffff; value++) {
                source.putShort((short) value);
                expected[offset++] = (byte) Math.round((value >>> 11) * 255.0 / 31);
                int green = format == Format.RGB565 ? (value >>> 5) & 63 : (value >>> 6) & 31;
                expected[offset++] = (byte) Math.round(green * 255.0 / (format == Format.RGB565 ? 63 : 31));
                int blue = format == Format.RGB565 ? value & 31 : (value >>> 1) & 31;
                expected[offset++] = (byte) Math.round(blue * 255.0 / 31);
                if (format == Format.RGB5A1) {
                    expected[offset++] = (byte) ((value & 1) == 0 ? 0 : 255);
                }
            }
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image(format, 256, 256, source),
                    GL.GL_TEXTURE_2D, 0, true);
            assertArrayEquals(expected, gl.uploads.get(0).data);
        }
    }

    @Test
    public void testNativePackedBytesIgnoreBufferOrderMetadata() {
        for (Format format : PACKED_FORMATS) {
            for (ByteOrder order : new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
                ByteBuffer source = packed(0xf800, 0x001f).order(order);
                final SourceState state = new SourceState(source);
                RecordingGl gl = new RecordingGl();
                gl.textureUtil(true, true).uploadTexture(image(format, 2, 1, source),
                        GL.GL_TEXTURE_2D, 0, true);
                assertArrayEquals(format == Format.RGB565 ? bytes(255, 0, 0, 0, 0, 255)
                        : bytes(255, 0, 0, 0, 0, 0, 123, 255), gl.uploads.get(0).data);
                state.assertUnchanged(source);
            }
        }
    }

    @Test
    public void testMipLevelsPreserveImageAndSourceState() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = packed(0, 0, 0, 0, 0xffff, 0xffff, 0xffff, 0xffff, 0xf800, 0xf800, 0);
            source.position(3).mark().limit(7);
            final SourceState state = new SourceState(source);
            Image image = image(format, 4, 2, source);
            int[] mipSizes = {16, 4, 2};
            image.setMipMapSizes(mipSizes);
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image, GL.GL_TEXTURE_2D, 0, true);
            assertEquals(3, gl.uploads.size());
            for (int level = 0; level < 3; level++) {
                Upload upload = gl.uploads.get(level);
                assertSrgb(upload, format);
                assertEquals(level, upload.level);
                assertEquals(Math.max(1, 4 >> level), upload.width);
                assertEquals(Math.max(1, 2 >> level), upload.height);
                assertEquals(mipSizes[level] / 2 * components(format), upload.data.length);
            }
            byte[] red = format == Format.RGB565 ? bytes(255, 0, 0, 255, 0, 0)
                    : bytes(255, 0, 0, 0, 255, 0, 0, 0);
            assertArrayEquals(red, gl.uploads.get(1).data);
            assertArrayEquals(new byte[components(format)], gl.uploads.get(2).data);
            assertSame(source, image.getData(0));
            assertSame(mipSizes, image.getMipMapSizes());
            assertArrayEquals(new int[]{16, 4, 2}, mipSizes);
            assertEquals(format, image.getFormat());
            assertEquals(ColorSpace.sRGB, image.getColorSpace());
            state.assertUnchanged(source);
            source.reset();
            assertEquals(3, source.position());
        }
    }

    @Test
    public void testReadOnlySourceAndCubemapFace() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = packed(0xffff).asReadOnlyBuffer();
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image(format, 1, 1, source),
                    GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, true);
            assertEquals(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, gl.uploads.get(0).target);
            assertArrayEquals(white(format, 1), gl.uploads.get(0).data);
        }
    }

    @Test
    public void testVolumeMipLevels() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = packed(0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0);
            Image image = new Image(format, 2, 2, 2, new ArrayList<>(Arrays.asList(source)), ColorSpace.sRGB);
            image.setMipMapSizes(new int[]{16, 2});
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image, GL2.GL_TEXTURE_3D, 0, true);
            assertEquals(2, gl.uploads.size());
            assertEquals(2, gl.uploads.get(0).depth);
            assertArrayEquals(white(format, 8), gl.uploads.get(0).data);
            assertEquals(1, gl.uploads.get(1).depth);
            assertArrayEquals(new byte[components(format)], gl.uploads.get(1).data);
        }
    }

    @Test
    public void testArrayAllocationAndSlices() {
        for (Format format : PACKED_FORMATS) {
            Image image = new Image(format, 2, 1, 0,
                    new ArrayList<>(Arrays.asList(packed(0, 0), packed(0xffff, 0xffff))), ColorSpace.sRGB);
            RecordingGl gl = new RecordingGl();
            TextureUtil util = gl.textureUtil(true, true);
            util.uploadTexture(image, GLExt.GL_TEXTURE_2D_ARRAY_EXT, -1, true);
            util.uploadTexture(image, GLExt.GL_TEXTURE_2D_ARRAY_EXT, 0, true);
            util.uploadTexture(image, GLExt.GL_TEXTURE_2D_ARRAY_EXT, 1, true);
            Upload allocation = gl.uploads.get(0);
            assertSrgb(allocation, format);
            assertNull(allocation.data);
            assertEquals(2, allocation.depth);
            assertEquals(0, gl.uploads.get(1).offsetZ);
            assertArrayEquals(new byte[2 * components(format)], gl.uploads.get(1).data);
            assertEquals(1, gl.uploads.get(2).offsetZ);
            assertArrayEquals(white(format, 2), gl.uploads.get(2).data);
        }
    }

    @Test
    public void testNullTextureAllocation() {
        for (Format format : PACKED_FORMATS) {
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image(format, 3, 2, null), GL.GL_TEXTURE_2D, 0, true);
            assertSrgb(gl.uploads.get(0), format);
            assertNull(gl.uploads.get(0).data);
        }
    }

    @Test
    public void testDeprecatedWholeSubImage() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = packed(0xffff, 0xffff);
            source.position(1).mark().limit(3);
            final SourceState state = new SourceState(source);
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadSubTexture(image(format, 2, 1, source),
                    GL.GL_TEXTURE_2D, 0, 7, 9, true);
            Upload upload = gl.uploads.get(0);
            assertEquals(GL.GL_UNSIGNED_BYTE, upload.type);
            assertEquals(7, upload.offsetX);
            assertEquals(9, upload.offsetY);
            assertArrayEquals(white(format, 2), upload.data);
            state.assertUnchanged(source);
            source.reset();
        }
    }

    @Test
    public void testCroppedSubImageWithAndWithoutRowLength() {
        for (Format format : PACKED_FORMATS) {
            for (boolean rowLength : new boolean[]{false, true}) {
                ByteBuffer source = packed(0, 0, 0, 0, 0xffff, 0xf800, 0, 0, 0xffff);
                source.position(2).mark().limit(16);
                final SourceState state = new SourceState(source);
                RecordingGl gl = new RecordingGl();
                gl.textureUtil(true, rowLength).uploadSubTexture(GL.GL_TEXTURE_2D,
                        image(format, 3, 3, source), 0, 7, 9, 1, 1, 2, 2, true);
                byte[] expected = format == Format.RGB565
                        ? bytes(255, 255, 255, 255, 0, 0, 0, 0, 0, 255, 255, 255)
                        : bytes(255, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 255);
                assertEquals(rowLength ? 1 : 2, gl.uploads.size());
                if (rowLength) {
                    assertArrayEquals(expected, gl.uploads.get(0).data);
                } else {
                    assertArrayEquals(Arrays.copyOfRange(expected, 0, expected.length / 2),
                            gl.uploads.get(0).data);
                    assertArrayEquals(Arrays.copyOfRange(expected, expected.length / 2, expected.length),
                            gl.uploads.get(1).data);
                    assertEquals(10, gl.uploads.get(1).offsetY);
                }
                assertEquals(0, gl.rowLength);
                assertEquals(7, gl.uploads.get(0).offsetX);
                assertEquals(9, gl.uploads.get(0).offsetY);
                state.assertUnchanged(source);
                source.reset();
            }
        }
    }

    @Test
    public void testRendererSubImageTransfersAcrossColorSpaces() {
        for (Format format : PACKED_FORMATS) {
            for (ColorSpace sourceSpace : ColorSpace.values()) {
                for (ColorSpace destinationSpace : ColorSpace.values()) {
                    for (boolean deprecated : new boolean[]{false, true}) {
                        RecordingGl gl = new RecordingGl();
                        final GLRenderer renderer = gl.renderer();
                        Image destination = image(format, 2, 1, packed(0, 0));
                        destination.setColorSpace(destinationSpace);
                        Texture2D texture = new Texture2D(destination);
                        ByteBuffer sourceData = packed(0x1863, 0xf801);
                        sourceData.position(1).mark();
                        final SourceState state = new SourceState(sourceData);
                        Image source = image(format, 2, 1, sourceData);
                        source.setColorSpace(sourceSpace);
                        if (deprecated) {
                            renderer.modifyTexture(texture, source, 0, 0);
                        } else {
                            renderer.modifyTexture(texture, source, 0, 0, 0, 0, 2, 1);
                        }
                        assertEquals(2, gl.uploads.size());
                        Upload definition = gl.uploads.get(0);
                        assertEquals(destinationSpace == ColorSpace.sRGB ? srgbInternal(format)
                                : format == Format.RGB565 ? GL.GL_RGB565 : GL.GL_RGB5_A1,
                                definition.internal);
                        Upload update = gl.uploads.get(1);
                        if (destinationSpace == ColorSpace.sRGB) {
                            assertEquals(GL.GL_UNSIGNED_BYTE, update.type);
                            assertArrayEquals(format == Format.RGB565 ? bytes(25, 12, 25, 255, 0, 8)
                                    : bytes(25, 8, 140, 255, 255, 0, 0, 255), update.data);
                        } else {
                            assertEquals(format == Format.RGB565 ? GL.GL_UNSIGNED_SHORT_5_6_5
                                    : GL.GL_UNSIGNED_SHORT_5_5_5_1, update.type);
                            assertArrayEquals(rawBytes(sourceData), update.data);
                        }
                        assertEquals(format == Format.RGB565 ? GL.GL_RGB : GL.GL_RGBA, update.format);
                        state.assertUnchanged(sourceData);
                        sourceData.reset();
                    }
                }
            }
        }
    }

    @Test
    public void testDesktopSubImagesKeepPackedTransfers() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = packed(0x1863, 0xf801);
            RecordingGl gl = new RecordingGl();
            TextureUtil util = gl.textureUtil(false, true);
            Image image = image(format, 2, 1, source);
            util.uploadSubTexture(image, GL.GL_TEXTURE_2D, 0, 0, 0, true);
            util.uploadSubTexture(GL.GL_TEXTURE_2D, image, 0, 0, 0, 0, 0, 2, 1, true);
            for (Upload upload : gl.uploads) {
                assertEquals(format == Format.RGB565 ? GL.GL_UNSIGNED_SHORT_5_6_5
                        : GL.GL_UNSIGNED_SHORT_5_5_5_1, upload.type);
                assertArrayEquals(rawBytes(source), upload.data);
            }
        }
    }

    @Test
    public void testDesktopLinearAndDisabledLinearizationKeepPackedTransfers() {
        for (Format format : PACKED_FORMATS) {
            for (int control = 0; control < 3; control++) {
                ByteBuffer source = packed(0x1863, 0xf801);
                Image image = image(format, 2, 1, source);
                if (control == 1) {
                    image.setColorSpace(ColorSpace.Linear);
                }
                RecordingGl gl = new RecordingGl();
                gl.textureUtil(control != 0, true).uploadTexture(image, GL.GL_TEXTURE_2D, 0, control != 2);
                Upload upload = gl.uploads.get(0);
                assertEquals(format == Format.RGB565 ? GL.GL_UNSIGNED_SHORT_5_6_5
                        : GL.GL_UNSIGNED_SHORT_5_5_5_1, upload.type);
                assertArrayEquals(rawBytes(source), upload.data);
                assertEquals(control == 0 ? srgbInternal(format)
                        : format == Format.RGB565 ? GL.GL_RGB565 : GL.GL_RGB5_A1, upload.internal);
            }
        }
    }

    @Test
    public void testOrdinarySrgbBytesRemainUnchanged() {
        for (Format format : new Format[]{Format.RGB8, Format.RGBA8}) {
            byte[] expected = format == Format.RGB8 ? bytes(12, 34, 56) : bytes(12, 34, 56, 78);
            ByteBuffer source = ByteBuffer.allocateDirect(expected.length);
            source.put(expected);
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image(format, 1, 1, source), GL.GL_TEXTURE_2D, 0, true);
            assertEquals(GL.GL_UNSIGNED_BYTE, gl.uploads.get(0).type);
            assertArrayEquals(expected, gl.uploads.get(0).data);
        }
    }

    @Test
    public void testSurplusCapacityIsNotConverted() {
        for (Format format : PACKED_FORMATS) {
            ByteBuffer source = ByteBuffer.allocateDirect(3).order(ByteOrder.nativeOrder());
            source.putShort((short) 0xffff).put((byte) 123);
            final SourceState state = new SourceState(source);
            RecordingGl gl = new RecordingGl();
            gl.textureUtil(true, true).uploadTexture(image(format, 1, 1, source), GL.GL_TEXTURE_2D, 0, true);
            assertArrayEquals(white(format, 1), gl.uploads.get(0).data);
            state.assertUnchanged(source);
        }
    }

    @Test
    public void testGles3WithoutCoreProfileAndWithoutSrgb() {
        for (Format format : PACKED_FORMATS) {
            for (boolean srgb : new boolean[]{false, true}) {
                EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30);
                if (srgb) {
                    caps.add(Caps.Srgb);
                }
                ByteBuffer source = packed(0xffff);
                RecordingGl gl = new RecordingGl();
                gl.textureUtil(caps).uploadTexture(image(format, 1, 1, source), GL.GL_TEXTURE_2D, 0, true);
                Upload upload = gl.uploads.get(0);
                if (srgb) {
                    assertSrgb(upload, format);
                    assertArrayEquals(white(format, 1), upload.data);
                } else {
                    assertEquals(format == Format.RGB565 ? GL.GL_UNSIGNED_SHORT_5_6_5
                            : GL.GL_UNSIGNED_SHORT_5_5_5_1, upload.type);
                    assertArrayEquals(rawBytes(source), upload.data);
                }
            }
        }
    }

    @Test
    public void testIncompletePackedPixelFailsWithoutChangingSource() {
        ByteBuffer source = ByteBuffer.allocateDirect(1);
        source.position(1).mark();
        final SourceState state = new SourceState(source);
        RecordingGl gl = new RecordingGl();
        TextureUtil util = gl.textureUtil(true, true);
        assertThrows(IllegalArgumentException.class,
                () -> util.uploadTexture(image(Format.RGB565, 1, 1, source), GL.GL_TEXTURE_2D, 0, true));
        assertTrue(gl.uploads.isEmpty());
        state.assertUnchanged(source);
        source.reset();
    }

    private static Image image(Format format, int width, int height, ByteBuffer source) {
        return new Image(format, width, height, source, ColorSpace.sRGB);
    }

    private static ByteBuffer packed(int... values) {
        ByteBuffer result = ByteBuffer.allocateDirect(values.length * 2).order(ByteOrder.nativeOrder());
        for (int value : values) {
            result.putShort((short) value);
        }
        result.flip();
        return result;
    }

    private static int components(Format format) {
        return format == Format.RGB565 ? 3 : 4;
    }

    private static int srgbInternal(Format format) {
        return format == Format.RGB565 ? GLExt.GL_SRGB8_EXT : GLExt.GL_SRGB8_ALPHA8_EXT;
    }

    private static void assertSrgb(Upload upload, Format format) {
        assertEquals(srgbInternal(format), upload.internal);
        assertEquals(format == Format.RGB565 ? GL.GL_RGB : GL.GL_RGBA, upload.format);
        assertEquals(GL.GL_UNSIGNED_BYTE, upload.type);
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private static byte[] rawBytes(ByteBuffer source) {
        ByteBuffer copy = source.duplicate();
        copy.clear();
        byte[] result = new byte[copy.remaining()];
        copy.get(result);
        return result;
    }

    private static byte[] white(Format format, int count) {
        byte[] result = new byte[components(format) * count];
        Arrays.fill(result, (byte) 255);
        return result;
    }

    private static final class SourceState {
        private final int position;
        private final int limit;
        private final ByteOrder order;
        private final byte[] data;

        private SourceState(ByteBuffer source) {
            position = source.position();
            limit = source.limit();
            order = source.order();
            data = rawBytes(source);
        }

        private void assertUnchanged(ByteBuffer source) {
            assertEquals(position, source.position());
            assertEquals(limit, source.limit());
            assertEquals(order, source.order());
            assertArrayEquals(data, rawBytes(source));
        }
    }

    private static final class Upload {
        private int target;
        private int level;
        private int internal;
        private int format;
        private int type;
        private int offsetX;
        private int offsetY;
        private int offsetZ;
        private int width;
        private int height;
        private int depth = 1;
        private boolean direct;
        private byte[] data;
    }

    private static final class RecordingGl implements InvocationHandler {
        private final List<Upload> uploads = new ArrayList<>();
        private int rowLength;

        private <T> T proxy(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this));
        }

        private TextureUtil textureUtil(boolean gles, boolean unpackRowLength) {
            EnumSet<Caps> caps = gles
                    ? EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30, Caps.CoreProfile, Caps.Srgb)
                    : EnumSet.of(Caps.OpenGL20, Caps.OpenGL30, Caps.CoreProfile, Caps.Srgb);
            if (unpackRowLength) {
                caps.add(Caps.UnpackRowLength);
            }
            return textureUtil(caps);
        }

        private TextureUtil textureUtil(EnumSet<Caps> caps) {
            TextureUtil util = new TextureUtil(proxy(GL.class), proxy(GL2.class), proxy(GLExt.class));
            util.initialize(caps);
            return util;
        }

        private GLRenderer renderer() {
            final GLRenderer renderer = new GLRenderer(proxy(GLES_30.class),
                    proxy(GLExt.class), proxy(GLFbo.class));
            renderer.initialize();
            assertTrue(renderer.getCaps().contains(Caps.OpenGLES30));
            assertTrue(renderer.getCaps().contains(Caps.Srgb));
            renderer.setLinearizeSrgbImages(true);
            return renderer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "glGetString":
                    int name = (Integer) args[0];
                    if (name == GL.GL_VERSION) {
                        return "OpenGL ES 3.0";
                    }
                    if (name == GL.GL_SHADING_LANGUAGE_VERSION) {
                        return "OpenGL ES GLSL ES 3.00";
                    }
                    if (name == GL.GL_EXTENSIONS) {
                        return "";
                    }
                    return "Packed-sRGB recording GL";
                case "glGetInteger":
                    int parameter = (Integer) args[0];
                    int value = parameter == GL3.GL_NUM_EXTENSIONS || parameter == GL.GL_FRAMEBUFFER_BINDING
                            || parameter == GLExt.GL_SAMPLE_BUFFERS_ARB || parameter == GLExt.GL_SAMPLES_ARB
                            ? 0 : 16;
                    ((IntBuffer) args[1]).put(0, value);
                    return null;
                case "glGenTextures":
                case "glGenVertexArrays":
                    ((IntBuffer) args[0]).put(0, 1);
                    return null;
                case "glPixelStorei":
                    if ((Integer) args[0] == GL.GL_UNPACK_ROW_LENGTH) {
                        rowLength = (Integer) args[1];
                    }
                    return null;
                case "glTexImage2D":
                case "glTexImage3D":
                case "glTexSubImage2D":
                case "glTexSubImage3D":
                    record(method.getName(), args);
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

        private void record(String method, Object[] args) {
            Upload upload = new Upload();
            upload.target = (Integer) args[0];
            upload.level = (Integer) args[1];
            boolean subImage = method.contains("Sub");
            boolean volume = method.endsWith("3D");
            int dimensionIndex;
            if (subImage) {
                upload.offsetX = (Integer) args[2];
                upload.offsetY = (Integer) args[3];
                if (volume) {
                    upload.offsetZ = (Integer) args[4];
                }
                dimensionIndex = volume ? 5 : 4;
            } else {
                upload.internal = (Integer) args[2];
                dimensionIndex = 3;
            }
            upload.width = (Integer) args[dimensionIndex];
            upload.height = (Integer) args[dimensionIndex + 1];
            if (volume) {
                upload.depth = (Integer) args[dimensionIndex + 2];
            }
            upload.format = (Integer) args[args.length - 3];
            upload.type = (Integer) args[args.length - 2];
            ByteBuffer source = (ByteBuffer) args[args.length - 1];
            if (source != null) {
                upload.direct = source.isDirect();
                int bpp = upload.type == GL.GL_UNSIGNED_BYTE ? (upload.format == GL.GL_RGB ? 3 : 4) : 2;
                int stride = rowLength == 0 ? upload.width : rowLength;
                upload.data = new byte[upload.width * upload.height * upload.depth * bpp];
                ByteBuffer copy = source.duplicate();
                for (int row = 0; row < upload.height * upload.depth; row++) {
                    copy.position(source.position() + row * stride * bpp);
                    copy.get(upload.data, row * upload.width * bpp, upload.width * bpp);
                }
            }
            uploads.add(upload);
        }
    }
}
