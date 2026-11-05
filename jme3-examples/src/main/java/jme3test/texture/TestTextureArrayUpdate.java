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
package jme3test.texture;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * Asset-free native validation of incremental array updates. Optional arguments
 * are the renderer name and output directory. Fails rather than skipping missing
 * capabilities. Creates a small PNG from actual array-layer framebuffer readback.
 */
public final class TestTextureArrayUpdate extends SimpleApplication {
    private final CountDownLatch finished = new CountDownLatch(1);
    private final Path output;
    private volatile Throwable failure;
    private int comparisons;

    private TestTextureArrayUpdate(Path output) {
        this.output = output;
    }

    public static void main(String[] args) throws Exception {
        String backend = args.length > 0 ? args[0] : AppSettings.LWJGL_OPENGL32;
        Path output = Paths.get(args.length > 1 ? args[1] : "build/texture-array-update");
        TestTextureArrayUpdate app = new TestTextureArrayUpdate(output);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(backend);
        settings.setResolution(320, 240);
        settings.setAudioRenderer(null);
        settings.setGammaCorrection(true);
        settings.setGraphicsDebug(true);
        if (args.length > 2 && "all-diagnostics".equals(args[2])) {
            settings.setGraphicsTiming(true);
            settings.setGraphicsTrace(true);
        }
        settings.setVSync(false);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
        boolean completed = app.finished.await(45, TimeUnit.SECONDS);
        if (!completed || app.failure != null) {
            // Failed initialization may leave no render thread to service a blocking stop.
            app.stop(false);
            throw new IllegalStateException(completed ? "Native array update probe failed"
                    : "Native array update probe timed out", app.failure);
        }
        app.stop(true);
        if (app.failure != null) {
            throw new IllegalStateException("Native array update probe cleanup failed", app.failure);
        }
    }

    @Override
    public void simpleInitApp() {
        try {
            flyCam.setEnabled(false);
            Renderer renderer = renderManager.getRenderer();
            if (!renderer.getCaps().contains(Caps.TextureArray)) {
                throw new IllegalStateException("Texture arrays are required for this probe");
            }
            normalizedLayers(renderer);
            floatingLayers(renderer);
            packedSrgbLayers(renderer);
            renderer.setFrameBuffer(null);
            System.out.println("NATIVE_ARRAY_UPDATE_PASS comparisons=" + comparisons
                    + " output=" + output.toAbsolutePath());
        } catch (Throwable error) {
            failure = error;
        } finally {
            finished.countDown();
        }
    }

    @Override
    public void handleError(String message, Throwable error) {
        failure = error == null ? new IllegalStateException(message) : error;
        System.err.println(message);
        failure.printStackTrace();
        finished.countDown();
    }

    private void normalizedLayers(Renderer renderer) throws IOException {
        int size = 8;
        byte[] first = rgba(size, size, 32, 24, 16, 255);
        byte[] second = rgba(size, size, 32, 64, 192, 255);
        TextureArray array = array(image(Image.Format.RGBA8, size, size, first, ColorSpace.Linear),
                image(Image.Format.RGBA8, size, size, second, ColorSpace.Linear));
        Image yellow = image(Image.Format.RGBA8, 3, 2, rgba(3, 2, 240, 200, 32, 255), ColorSpace.Linear);
        Image violet = image(Image.Format.RGBA8, 1, 1, rgba(1, 1, 180, 32, 240, 255), ColorSpace.Linear);
        ByteBuffer source = yellow.getData(0);
        source.position(1).mark().limit(source.capacity() - 1);
        renderer.modifyTexture(array, yellow, 1, 2, 3);
        renderer.modifyTexture(array, violet, 1, 0, 0);
        require(source.position() == 1 && source.limit() == source.capacity() - 1, "source position/limit");
        source.reset();
        require(source.position() == 1, "source mark");
        byte[] expected = second.clone();
        patch(expected, size, yellow, 2, 3);
        patch(expected, size, violet, 0, 0);
        byte[] actualFirst = readBytes(renderer, array, 0);
        byte[] actualSecond = readBytes(renderer, array, 1);
        equal(first, actualFirst, "untouched layer");
        equal(expected, actualSecond, "updated rectangle and untouched texels");
        equal(second, bytes(array.getImage().getData(1)), "destination CPU bytes remain unchanged");
        require(!array.getImage().isUpdateNeeded(), "resident update must not mark full image dirty");
        writePreview(actualFirst, actualSecond, size);

        // GPU-only patches are lost on recreation; retaining/replaying them is caller-owned.
        renderer.deleteImage(array.getImage());
        equal(second, readBytes(renderer, array, 1), "recreation uses authoritative CPU backing");
        renderer.modifyTexture(array, yellow, 1, 2, 3);
        renderer.modifyTexture(array, violet, 1, 0, 0);
        equal(expected, readBytes(renderer, array, 1), "caller replay after recreation");
        equal(first, readBytes(renderer, array, 0), "replay preserves other layer");
        renderer.deleteImage(array.getImage());
    }

    private void floatingLayers(Renderer renderer) {
        Image first = floats(4, 4, 123.25f);
        Image second = floats(4, 4, -45.5f);
        TextureArray array = array(first, second);
        Image patch = floats(2, 1, 2000.125f);
        patch.getData(0).putFloat(4, -.125f);
        renderer.modifyTexture(array, patch, 1, 1, 2);
        ByteBuffer untouched = readFloats(renderer, array, 0);
        ByteBuffer modified = readFloats(renderer, array, 1);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int offset = (y * 4 + x) * 16;
                require(untouched.getFloat(offset) == 123.25f, "untouched R32F height");
                float expected = y == 2 && x == 1 ? 2000.125f : y == 2 && x == 2 ? -.125f : -45.5f;
                require(modified.getFloat(offset) == expected, "exact R32F patch height");
            }
        }
        renderer.deleteImage(array.getImage());
    }

    private void packedSrgbLayers(Renderer renderer) {
        Image red = packed(2, 2, 0xf801);
        TextureArray array = array(red, packed(2, 2, 0xf801));
        renderer.modifyTexture(array, packed(1, 1, 0x07c1), 1, 1, 0);
        byte[] expected = rgba(2, 2, 255, 0, 0, 255);
        expected[4] = 0;
        expected[5] = (byte) 255;
        equal(expected, readBytes(renderer, array, 1), "packed sRGB patch and untouched texels");
        equal(rgba(2, 2, 255, 0, 0, 255), readBytes(renderer, array, 0), "untouched packed sRGB layer");
        renderer.deleteImage(array.getImage());
    }

    private static TextureArray array(Image... layers) {
        TextureArray array = new TextureArray(Arrays.asList(layers));
        array.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        array.setMagFilter(Texture.MagFilter.Nearest);
        return array;
    }

    private static Image image(Image.Format format, int width, int height, byte[] bytes, ColorSpace space) {
        ByteBuffer data = BufferUtils.createByteBuffer(bytes.length);
        data.put(bytes).flip();
        return new Image(format, width, height, data, space);
    }

    private static Image floats(int width, int height, float value) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4).order(ByteOrder.nativeOrder());
        for (int i = 0; i < width * height; i++) {
            data.putFloat(value);
        }
        data.flip();
        return new Image(Image.Format.Luminance32F, width, height, data, ColorSpace.Linear);
    }

    private static Image packed(int width, int height, int value) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 2).order(ByteOrder.nativeOrder());
        for (int i = 0; i < width * height; i++) {
            data.putShort((short) value);
        }
        data.flip();
        return new Image(Image.Format.RGB5A1, width, height, data, ColorSpace.sRGB);
    }

    private static FrameBuffer framebuffer(TextureArray array, int layer) {
        Image image = array.getImage();
        FrameBuffer framebuffer = new FrameBuffer(image.getWidth(), image.getHeight(), 1);
        framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(array).layer(layer));
        return framebuffer;
    }

    private static byte[] readBytes(Renderer renderer, TextureArray array, int layer) {
        FrameBuffer framebuffer = framebuffer(array, layer);
        ByteBuffer data = BufferUtils.createByteBuffer(array.getImage().getWidth() * array.getImage().getHeight() * 4);
        try {
            renderer.readFrameBufferWithFormat(framebuffer, data, Image.Format.RGBA8);
            return bytes(data);
        } finally {
            renderer.setFrameBuffer(null);
            renderer.deleteFrameBuffer(framebuffer);
        }
    }

    private static ByteBuffer readFloats(Renderer renderer, TextureArray array, int layer) {
        FrameBuffer framebuffer = framebuffer(array, layer);
        ByteBuffer data = BufferUtils.createByteBuffer(array.getImage().getWidth() * array.getImage().getHeight() * 16)
                .order(ByteOrder.nativeOrder());
        try {
            renderer.readFrameBufferWithFormat(framebuffer, data, Image.Format.RGBA32F);
            return data;
        } finally {
            renderer.setFrameBuffer(null);
            renderer.deleteFrameBuffer(framebuffer);
        }
    }

    private static byte[] rgba(int width, int height, int r, int g, int b, int a) {
        byte[] bytes = new byte[width * height * 4];
        for (int i = 0; i < bytes.length; i += 4) {
            bytes[i] = (byte) r;
            bytes[i + 1] = (byte) g;
            bytes[i + 2] = (byte) b;
            bytes[i + 3] = (byte) a;
        }
        return bytes;
    }

    private static byte[] bytes(ByteBuffer data) {
        ByteBuffer copy = data.duplicate();
        copy.clear();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return bytes;
    }

    private static void patch(byte[] dest, int width, Image source, int x, int y) {
        byte[] data = bytes(source.getData(0));
        for (int row = 0; row < source.getHeight(); row++) {
            System.arraycopy(data, row * source.getWidth() * 4,
                    dest, ((y + row) * width + x) * 4, source.getWidth() * 4);
        }
    }

    private void require(boolean condition, String description) {
        comparisons++;
        if (!condition) {
            throw new IllegalStateException(description);
        }
    }

    private void equal(byte[] expected, byte[] actual, String description) {
        require(Arrays.equals(expected, actual), description);
    }

    private void writePreview(byte[] first, byte[] second, int size) throws IOException {
        BufferedImage image = new BufferedImage(size * 2, size, BufferedImage.TYPE_INT_ARGB);
        for (int layer = 0; layer < 2; layer++) {
            byte[] data = layer == 0 ? first : second;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int offset = (y * size + x) * 4;
                    int color = ((data[offset + 3] & 255) << 24) | ((data[offset] & 255) << 16)
                            | ((data[offset + 1] & 255) << 8) | (data[offset + 2] & 255);
                    image.setRGB(layer * size + x, size - y - 1, color);
                }
            }
        }
        Files.createDirectories(output);
        ImageIO.write(image, "png", output.resolve("array-layers.png").toFile());
    }
}
