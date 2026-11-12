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
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Verifies managed base-level array-layer uploads without a native GL context. */
public class TextureArrayUpdateTest {

    private final List<RecordingGl> contexts = new ArrayList<>();

    @AfterEach
    public void cleanupRenderers() {
        for (RecordingGl gl : new ArrayList<>(contexts)) {
            gl.close();
        }
    }

    @Test
    public void standardArrayConstructionRequestsMipmapsOnlyForMipFilteredTextures() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        Image first = image(Image.Format.RGBA8, 2, 2, bytes(16, (byte) 1), ColorSpace.Linear);
        Image second = image(Image.Format.RGBA8, 2, 2, bytes(16, (byte) 2), ColorSpace.Linear);
        TextureArray array = new TextureArray(Arrays.asList(first, second));
        assertFalse(array.getImage().isGeneratedMipmapsRequired());
        renderer.modifyTexture(array, first, 1, 0, 0);
        assertEquals(3, gl.subImage3dCalls);
        array.setMinFilter(Texture.MinFilter.Trilinear);
        assertTrue(array.getImage().isGeneratedMipmapsRequired());
    }

    @Test
    public void updatesOnlyRequestedLayerAndRectangleAndKeepsImageGpuOnly() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        ArrayList<ByteBuffer> initialLayers = new ArrayList<>(Arrays.asList(
                bytes(64, (byte) 0x11), bytes(64, (byte) 0x22)));
        TextureArray destination = textureArray(Image.Format.RGBA8, 4, 4, initialLayers, ColorSpace.Linear);
        final byte[][] original = {contents(initialLayers.get(0)), contents(initialLayers.get(1))};
        Image patch = image(Image.Format.RGBA8, 2, 2, bytes(16, (byte) 0x7a), ColorSpace.Linear);
        ByteBuffer source = patch.getData(0);
        source.position(3).mark().position(5).limit(9);
        final SourceState sourceState = new SourceState(source);
        gl.clearCalls();

        renderer.modifyTexture(destination, patch, 1, 1, 2);

        assertEquals(1, gl.arrayAllocations.size());
        assertEquals(3, gl.subImage3dCalls, "two initial slices and one patch are uploaded");
        Upload update = gl.lastSubImage();
        assertEquals(GLExt.GL_TEXTURE_2D_ARRAY_EXT, update.target);
        assertEquals(0, update.level);
        assertEquals(1, update.offsetX);
        assertEquals(2, update.offsetY);
        assertEquals(1, update.offsetZ);
        assertEquals(2, update.width);
        assertEquals(2, update.height);
        assertEquals(1, update.depth);
        assertArrayEquals(contents(source), update.data);
        sourceState.assertUnchanged(source);
        source.reset();
        assertEquals(3, source.position(), "the caller's mark is retained");
        source.position(5);
        assertArrayEquals(original[0], contents(initialLayers.get(0)));
        assertArrayEquals(original[1], contents(initialLayers.get(1)));
        assertFalse(destination.getImage().isUpdateNeeded());

        gl.clearCalls();
        renderer.modifyTexture(destination, patch, 0, 0, 1);
        assertEquals(0, gl.arrayAllocations.size(), "resident updates do not reallocate storage");
        assertEquals(1, gl.subImage3dCalls, "resident update uploads only its requested patch");
        assertArrayEquals(original[0], contents(initialLayers.get(0)));
        assertArrayEquals(original[1], contents(initialLayers.get(1)));
        sourceState.assertUnchanged(source);

        source.position(3).mark().position(5).limit(9);
        SourceState beforeFailure = new SourceState(source);
        gl.failNextSubImage = true;
        assertThrows(IllegalStateException.class,
                () -> renderer.modifyTexture(destination, patch, 0, 0, 1));
        beforeFailure.assertUnchanged(source);
        source.reset();
        assertEquals(3, source.position(), "failed uploads preserve the caller's mark too");
    }

    @Test
    public void uploadsFloatPatchesWithFloatPixelType() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        renderer.setLinearizeSrgbImages(false);
        ArrayList<ByteBuffer> layers = new ArrayList<>(Arrays.asList(
                floats(4, 1f), floats(4, 2f)));
        TextureArray destination = textureArray(Image.Format.R32F, 2, 2, layers, ColorSpace.Linear);
        Image patch = image(Image.Format.R32F, 1, 2, floats(2, 3.5f), ColorSpace.Linear);
        gl.clearCalls();

        renderer.modifyTexture(destination, patch, 0, 1, 0);

        Upload update = gl.lastSubImage();
        assertEquals(GL.GL_RED, update.format);
        assertEquals(GL.GL_FLOAT, update.type);
        assertEquals(1, update.offsetX);
        assertEquals(0, update.offsetY);
        assertEquals(0, update.offsetZ);
        assertEquals(1, update.width);
        assertEquals(2, update.height);
        assertEquals(1, update.depth);
        assertEquals(8, update.data.length);
    }

    @Test
    public void expandsPackedSrgbPatchForArrayLayer() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        renderer.setLinearizeSrgbImages(true);
        ArrayList<ByteBuffer> layers = new ArrayList<>(Arrays.asList(
                shorts(2, 0), shorts(2, 0xffff)));
        TextureArray destination = textureArray(Image.Format.RGB565, 2, 1, layers, ColorSpace.sRGB);
        Image patch = image(Image.Format.RGB565, 2, 1, shorts(2, 0xf800, 0x07e0), ColorSpace.sRGB);
        gl.clearCalls();

        renderer.modifyTexture(destination, patch, 1, 0, 0);

        Upload update = gl.lastSubImage();
        assertEquals(GL.GL_RGB, update.format);
        assertEquals(GL.GL_UNSIGNED_BYTE, update.type);
        assertEquals(1, update.offsetZ);
        assertArrayEquals(new byte[]{(byte) 255, 0, 0, 0, (byte) 255, 0}, update.data);
    }

    @Test
    public void arrayUpdateActivatesUnitZeroAndTargetsItsDestination() throws TextureUnitException {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        Image patch = image(Image.Format.RGBA8, 1, 1, bytes(4, (byte) 0x5a), ColorSpace.Linear);

        TextureArray destinationArray = textureArray(Image.Format.RGBA8, 2, 2,
                new ArrayList<>(Arrays.asList(bytes(16, (byte) 3), bytes(16, (byte) 4))), ColorSpace.Linear);
        assertSubImageTargetsUnitZero(gl, renderer, destinationArray, () ->
                renderer.modifyTexture(destinationArray, patch, 1, 0, 0));
    }

    private void assertSubImageTargetsUnitZero(RecordingGl gl, GLRenderer renderer,
            Texture destination, Runnable update) throws TextureUnitException {
        renderer.setTexture(0, destination);
        Texture2D otherTexture = new Texture2D(image(
                Image.Format.RGBA8, 1, 1, bytes(4, (byte) 0x33), ColorSpace.Linear));
        renderer.setTexture(3, otherTexture);
        gl.clearCalls();

        update.run();

        Upload upload = gl.lastSubImage();
        assertEquals(GL.GL_TEXTURE0, upload.activeTexture,
                "subimage upload must reactivate unit zero after cached setTexture binding");
        assertEquals(destination.getImage().getId(), upload.boundTexture,
                "subimage upload must target the destination bound on unit zero");
    }

    @Test
    public void residentPatchDoesNotRequireCpuBacking() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        ArrayList<ByteBuffer> layers = new ArrayList<>(Arrays.asList(
                bytes(16, (byte) 0), bytes(16, (byte) 1)));
        TextureArray destination = textureArray(Image.Format.RGBA8, 2, 2, layers, ColorSpace.Linear);
        Image patch = image(Image.Format.RGBA8, 1, 1, bytes(4, (byte) 2), ColorSpace.Linear);
        renderer.modifyTexture(destination, patch, 0, 0, 0); // allocate and upload the baseline

        // Discard CPU pixels without changing the resident storage or layer count.
        layers.replaceAll(data -> null);
        gl.clearCalls();
        renderer.modifyTexture(destination, patch, 1, 1, 1);

        assertEquals(1, gl.subImage3dCalls, "resident update uploads only its source patch");
        assertEquals(0, gl.arrayAllocations.size(), "discarding CPU pixels must not reallocate GPU storage");
        Upload update = gl.lastSubImage();
        assertEquals(1, update.offsetZ);
        assertEquals(1, update.offsetX);
        assertEquals(1, update.offsetY);
        assertArrayEquals(contents(patch.getData(0)), update.data);
        assertFalse(destination.getImage().isUpdateNeeded());

        // A later full upload still needs complete CPU backing, before any GL mutation.
        destination.getImage().setUpdateNeeded();
        gl.clearCalls();
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, patch, 1, 1, 1));
        assertTrue(gl.calls.isEmpty(), "missing full-upload data must fail before GL calls");
    }

    @Test
    public void prevalidatesMalformedInputsAndMissingArrayCapability() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        TextureArray destination = textureArray(Image.Format.RGBA8, 2, 2,
                new ArrayList<>(Arrays.asList(bytes(16, (byte) 0), bytes(16, (byte) 1))), ColorSpace.Linear);
        Image patch = image(Image.Format.RGBA8, 1, 1, bytes(4, (byte) 2), ColorSpace.Linear);
        List<Runnable> invalid = Arrays.asList(
                () -> renderer.modifyTexture(destination, patch, -1, 0, 0),
                () -> renderer.modifyTexture(destination, patch, 2, 0, 0),
                () -> renderer.modifyTexture(destination, patch, 1, 2, 0),
                () -> renderer.modifyTexture(destination, patch, 1, Integer.MAX_VALUE, 0));
        gl.clearCalls();
        for (Runnable invalidCall : invalid) {
            assertThrows(IllegalArgumentException.class, invalidCall::run);
            assertTrue(gl.calls.isEmpty(), "invalid input must fail before any GL call");
        }

        for (int[] size : new int[][]{{0, 1}, {-1, 1}, {Integer.MAX_VALUE, Integer.MAX_VALUE}}) {
            Image malformed = image(Image.Format.RGBA8, size[0], size[1],
                    bytes(4, (byte) 0), ColorSpace.Linear);
            assertThrows(IllegalArgumentException.class,
                    () -> renderer.modifyTexture(destination, malformed, 0, 0, 0));
            assertTrue(gl.calls.isEmpty(), "invalid dimensions must fail before GL calls");
        }
        Image aliasesFormat = image(Image.Format.ABGR8, 1, 1, bytes(4, (byte) 0), ColorSpace.Linear);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, aliasesFormat, 0, 0, 0));
        assertTrue(gl.calls.isEmpty(), "equal pixel size is insufficient for format compatibility");

        Image shortImage = image(Image.Format.RGBA8, 2, 2, bytes(3, (byte) 3), ColorSpace.Linear);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, shortImage, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());
        Image heapImage = image(Image.Format.RGBA8, 1, 1,
                ByteBuffer.allocate(4), ColorSpace.Linear);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, heapImage, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());
        Image mismatchedFormat = image(Image.Format.RGB8, 1, 1, bytes(3, (byte) 4), ColorSpace.Linear);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, mismatchedFormat, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());
        Image mismatchedColorSpace = image(Image.Format.RGBA8, 1, 1,
                bytes(4, (byte) 4), ColorSpace.sRGB);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(destination, mismatchedColorSpace, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());

        Image mipPatch = image(Image.Format.RGBA8, 2, 2, bytes(20, (byte) 5), ColorSpace.Linear);
        mipPatch.setMipMapSizes(new int[]{16, 4});
        assertThrows(UnsupportedOperationException.class,
                () -> renderer.modifyTexture(destination, mipPatch, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());

        TextureArray mipFiltered = textureArray(Image.Format.RGBA8, 2, 2,
                new ArrayList<>(Arrays.asList(bytes(16, (byte) 0), bytes(16, (byte) 1))), ColorSpace.Linear);
        mipFiltered.setMinFilter(Texture.MinFilter.Trilinear);
        assertThrows(UnsupportedOperationException.class,
                () -> renderer.modifyTexture(mipFiltered, patch, 0, 0, 0));
        assertTrue(gl.calls.isEmpty());

        TextureArray incomplete = textureArray(Image.Format.RGBA8, 2, 2,
                new ArrayList<>(Arrays.asList(bytes(16, (byte) 0), bytes(3, (byte) 1))), ColorSpace.Linear);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.modifyTexture(incomplete, patch, 0, 0, 0));
        assertTrue(gl.calls.isEmpty(), "all backing layers are checked before the initial upload");

        RecordingGl gles2 = new RecordingGl("OpenGL ES 2.0");
        GLRenderer noArrayRenderer = gles2.renderer(false);
        assertFalse(noArrayRenderer.getCaps().contains(Caps.TextureArray));
        gles2.clearCalls();
        assertThrows(UnsupportedOperationException.class,
                () -> noArrayRenderer.modifyTexture(destination, patch, 0, 0, 0));
        assertTrue(gles2.calls.isEmpty(), "unsupported capability must fail before GL calls");
    }

    @Test
    public void gpuPatchIsReplayedAfterImageNativeObjectReset() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        ArrayList<ByteBuffer> layers = new ArrayList<>(Arrays.asList(
                bytes(4, (byte) 0x10), bytes(4, (byte) 0x20)));
        TextureArray destination = textureArray(Image.Format.RGBA8, 1, 1, layers, ColorSpace.Linear);
        Image patch = image(Image.Format.RGBA8, 1, 1, bytes(4, (byte) 0x66), ColorSpace.Linear);
        final byte[][] before = {contents(layers.get(0)), contents(layers.get(1))};

        renderer.modifyTexture(destination, patch, 1, 0, 0);
        assertEquals(1, gl.arrayAllocations.size());
        assertEquals(3, gl.subImage3dCalls, "two baseline layers and the patch are uploaded");
        renderer.deleteImage(destination.getImage());
        gl.clearCalls();

        // Context recreation reuploads the CPU baseline; callers replay GPU-only patches.
        renderer.modifyTexture(destination, patch, 1, 0, 0);

        assertEquals(1, gl.arrayAllocations.size());
        assertEquals(3, gl.subImage3dCalls, "recreation restores both CPU slices before replaying the patch");
        assertTrue(gl.calls.contains("glBindTexture"), "recreated storage needs a native rebind");
        assertArrayEquals(before[0], contents(layers.get(0)));
        assertArrayEquals(before[1], contents(layers.get(1)));
    }

    @Test
    public void dirtyArrayRestoresBaselineBeforeApplyingPatchAndPreservesAliasedSource() {
        RecordingGl gl = new RecordingGl("OpenGL ES 3.0");
        GLRenderer renderer = gl.renderer(true);
        ByteBuffer first = bytes(16, (byte) 1);
        ByteBuffer second = bytes(16, (byte) 2);
        TextureArray destination = textureArray(Image.Format.RGBA8, 2, 2,
                new ArrayList<>(Arrays.asList(first, second)), ColorSpace.Linear);
        Image patch = image(Image.Format.RGBA8, 2, 2, second, ColorSpace.Linear);
        second.position(2).mark().position(3).limit(7);
        SourceState state = new SourceState(second);
        renderer.modifyTexture(destination, patch, 0, 0, 0);
        state.assertUnchanged(second);
        second.reset();
        assertEquals(2, second.position());
        second.position(3);
        destination.getImage().setUpdateNeeded();
        gl.clearCalls();

        renderer.modifyTexture(destination, patch, 0, 0, 0);

        assertEquals(1, gl.arrayAllocations.size());
        assertEquals(3, gl.subImage3dCalls, "dirty arrays restore both baseline layers before the patch");
        assertArrayEquals(contents(second), gl.lastSubImage().data);
        assertFalse(destination.getImage().isUpdateNeeded());
        state.assertUnchanged(second);
    }

    private static TextureArray textureArray(Image.Format format, int width, int height,
                                              ArrayList<ByteBuffer> layers, ColorSpace colorSpace) {
        TextureArray texture = new TextureArray();
        texture.setImage(new Image(format, width, height, 0, layers, colorSpace));
        return texture;
    }

    private static Image image(Image.Format format, int width, int height,
                               ByteBuffer data, ColorSpace colorSpace) {
        return new Image(format, width, height, data, colorSpace);
    }

    private static ByteBuffer bytes(int count, byte value) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(count);
        while (buffer.hasRemaining()) {
            buffer.put(value);
        }
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer floats(int count, float value) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(count * Float.BYTES).order(ByteOrder.nativeOrder());
        FloatBuffer floats = buffer.asFloatBuffer();
        while (floats.hasRemaining()) {
            floats.put(value);
        }
        return buffer;
    }

    private static ByteBuffer shorts(int count, int... values) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(count * Short.BYTES).order(ByteOrder.nativeOrder());
        for (int value : values) {
            buffer.putShort((short) value);
        }
        buffer.flip();
        return buffer;
    }

    private static byte[] contents(ByteBuffer buffer) {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.clear();
        byte[] result = new byte[duplicate.remaining()];
        duplicate.get(result);
        return result;
    }

    private static final class SourceState {
        private final int position;
        private final int limit;
        private final ByteOrder order;
        private final byte[] contents;

        private SourceState(ByteBuffer source) {
            position = source.position();
            limit = source.limit();
            order = source.order();
            contents = TextureArrayUpdateTest.contents(source);
        }

        private void assertUnchanged(ByteBuffer source) {
            assertEquals(position, source.position());
            assertEquals(limit, source.limit());
            assertEquals(order, source.order());
            assertArrayEquals(contents, TextureArrayUpdateTest.contents(source));
        }
    }

    private static final class Upload {
        int target;
        int level;
        int internal;
        int format;
        int type;
        int offsetX;
        int offsetY;
        int offsetZ;
        int width;
        int height;
        int depth = 1;
        int activeTexture;
        int boundTexture;
        byte[] data;
    }

    private final class RecordingGl implements InvocationHandler, AutoCloseable {
        private final String version;
        private final List<String> calls = new ArrayList<>();
        private final List<Upload> arrayAllocations = new ArrayList<>();
        private final List<Upload> subImages = new ArrayList<>();
        private final Map<String, Integer> boundTextures = new HashMap<>();
        private int activeTexture = GL.GL_TEXTURE0;
        private boolean closed;
        private boolean failNextSubImage;
        private int subImage3dCalls;
        private int nextTextureId = 1;
        private GLRenderer renderer;

        private RecordingGl(String version) {
            this.version = version;
            contexts.add(this);
        }

        private GLRenderer renderer(boolean textureArray) {
            renderer = new GLRenderer(version.contains("3.0")
                    ? proxy(GL.class, GL2.class, GLES_30.class)
                    : proxy(GL.class, GL2.class), proxy(GLExt.class), proxy(GLFbo.class));
            renderer.initialize();
            if (textureArray) {
                assertTrue(renderer.getCaps().contains(Caps.TextureArray));
            }
            return renderer;
        }

        private void clearCalls() {
            calls.clear();
            arrayAllocations.clear();
            subImages.clear();
            subImage3dCalls = 0;
        }

        private Upload lastSubImage() {
            return subImages.get(subImages.size() - 1);
        }

        private <T> T proxy(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this));
        }

        private GL proxy(Class<?>... types) {
            return (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), types, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            calls.add(name);
            switch (name) {
                case "glGetString":
                    int stringName = (Integer) args[0];
                    if (stringName == GL.GL_VERSION) {
                        return version;
                    }
                    if (stringName == GL.GL_SHADING_LANGUAGE_VERSION) {
                        return version.contains("3.0") ? "OpenGL ES GLSL ES 3.00" : "OpenGL ES GLSL ES 1.00";
                    }
                    if (stringName == GL.GL_EXTENSIONS) {
                        return "";
                    }
                    return "TextureArrayUpdateTest";
                case "glGetInteger":
                    int parameter = (Integer) args[0];
                    int value = parameter == GL3.GL_NUM_EXTENSIONS || parameter == GL.GL_FRAMEBUFFER_BINDING
                            || parameter == GLExt.GL_SAMPLE_BUFFERS_ARB || parameter == GLExt.GL_SAMPLES_ARB
                            ? 0 : 16;
                    ((IntBuffer) args[1]).put(0, value);
                    return null;
                case "glGetFloat":
                    ((FloatBuffer) args[1]).put(0, 1f);
                    return null;
                case "glGenTextures":
                    ((IntBuffer) args[0]).put(0, nextTextureId++);
                    return null;
                case "glActiveTexture":
                    activeTexture = (Integer) args[0];
                    return null;
                case "glBindTexture":
                    boundTextures.put(bindingKey(activeTexture, (Integer) args[0]), (Integer) args[1]);
                    return null;
                case "glGenVertexArrays":
                    ((IntBuffer) args[0]).put(0, 1);
                    return null;
                case "glTexImage3D":
                    Upload allocation = record(name, args);
                    arrayAllocations.add(allocation);
                    return null;
                case "glTexSubImage2D":
                case "glTexSubImage3D":
                    Upload upload = record(name, args);
                    if (name.endsWith("3D")) {
                        subImage3dCalls++;
                    }
                    subImages.add(upload);
                    if (failNextSubImage) {
                        failNextSubImage = false;
                        ((ByteBuffer) args[args.length - 1]).position(1);
                        throw new IllegalStateException("simulated native upload failure");
                    }
                    return null;
                default:
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    if (method.getReturnType() == float.class) {
                        return 0f;
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    throw new AssertionError("Unexpected GL call: " + method);
            }
        }

        private Upload record(String name, Object[] args) {
            Upload upload = new Upload();
            upload.target = (Integer) args[0];
            upload.level = (Integer) args[1];
            upload.activeTexture = activeTexture;
            upload.boundTexture = boundTextures.getOrDefault(bindingKey(activeTexture, upload.target), 0);
            boolean subImage = name.contains("Sub");
            if (subImage && name.endsWith("3D")) {
                upload.offsetX = (Integer) args[2];
                upload.offsetY = (Integer) args[3];
                upload.offsetZ = (Integer) args[4];
                upload.width = (Integer) args[5];
                upload.height = (Integer) args[6];
                upload.depth = (Integer) args[7];
                upload.format = (Integer) args[8];
                upload.type = (Integer) args[9];
            } else if (subImage) {
                upload.offsetX = (Integer) args[2];
                upload.offsetY = (Integer) args[3];
                upload.width = (Integer) args[4];
                upload.height = (Integer) args[5];
                upload.format = (Integer) args[6];
                upload.type = (Integer) args[7];
            } else {
                upload.internal = (Integer) args[2];
                upload.width = (Integer) args[3];
                upload.height = (Integer) args[4];
                upload.depth = (Integer) args[5];
                upload.format = (Integer) args[7];
                upload.type = (Integer) args[8];
            }
            ByteBuffer data = (ByteBuffer) args[args.length - 1];
            if (data != null) {
                ByteBuffer copy = data.duplicate();
                byte[] bytes = new byte[copy.remaining()];
                copy.get(bytes);
                upload.data = bytes;
            }
            return upload;
        }

        private String bindingKey(int unit, int target) {
            return unit + ":" + target;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (renderer != null) {
                renderer.cleanup();
            }
            contexts.remove(this);
        }
    }
}
