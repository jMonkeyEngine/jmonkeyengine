/*
 * Copyright (c) 2026 jMonkeyEngine
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.jmonkeyengine.screenshottests.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ContrastAdjustmentFilter;
import com.jme3.post.filters.CrossHatchFilter;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengles.GLES20;

/** Verify post-filter sampler types and output with single- and multisampled input. */
@Tag("integration")
class TestMsaaFilterSampling {
    private static final int SIZE = 64;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void multisamplingPreservesFilterOutput(boolean hatch) throws Exception {
        byte[] single = render(hatch, 1);
        byte[] multisampled = render(hatch, 4);
        assertEquals(single.length, multisampled.length);
        int brightest = 0;
        for (int i = 0; i < single.length; i++) {
            if (i % 4 == 3) {
                continue;
            }
            int expected = single[i] & 255;
            brightest = Math.max(brightest, expected);
            assertEquals(expected, multisampled[i] & 255, 1,
                    "MSAA changed a post-filter color component at byte " + i);
        }
        assertTrue(brightest > 32, "single-sample control must contain visible output");
    }

    private static void verifySampler(boolean es, int samples) {
        int program = es ? GLES20.glGetInteger(GL20.GL_CURRENT_PROGRAM)
                : GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int count = es ? GLES20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS)
                : GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);
        try {
            for (int i = 0; i < count; i++) {
                String name = es ? GLES20.glGetActiveUniform(program, i, 256, size, type)
                        : GL20.glGetActiveUniform(program, i, 256, size, type);
                if (name.equals("m_Texture")) {
                    assertEquals(samples > 1 ? GL32.GL_SAMPLER_2D_MULTISAMPLE : GL20.GL_SAMPLER_2D,
                            type.get(0), "compiled filter sampler must match its actual input texture");
                    return;
                }
            }
            throw new AssertionError("filter texture sampler was not active");
        } finally {
            BufferUtils.destroyDirectBuffer(size);
            BufferUtils.destroyDirectBuffer(type);
        }
    }

    private byte[] render(boolean hatch, int samples) throws Exception {
        CompletableFuture<byte[]> result = new CompletableFuture<>();
        AtomicReference<Thread> renderThread = new AtomicReference<>();
        final SimpleApplication app = new SimpleApplication(new AppState[0]) {
            private int frames;
            private FilterPostProcessor processor;
            private FrameBuffer output;
            private boolean checked;

            @Override
            public void simpleInitApp() {
                renderThread.set(Thread.currentThread());
                viewPort.setBackgroundColor(new ColorRGBA(0.25f, 0.5f, 0.75f, 1f));
                output = new FrameBuffer(SIZE, SIZE, 1);
                output.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
                viewPort.setOutputFrameBuffer(output);
                processor = new FilterPostProcessor(assetManager);
                processor.setFrameBufferFormat(Image.Format.RGBA8);
                processor.setNumSamples(samples);
                processor.addFilter(hatch ? new CrossHatchFilter() {
                    @Override
                    protected void postFilter(Renderer r, FrameBuffer buffer) {
                        checkProgram();
                    }
                } : new ContrastAdjustmentFilter() {
                    @Override
                    protected void postFilter(Renderer r, FrameBuffer buffer) {
                        checkProgram();
                    }
                });
                viewPort.addProcessor(processor);
            }

            private void checkProgram() {
                if (checked) {
                    return;
                }
                checked = true;
                int actualSamples = processor.getFilterTexture().getImage().getMultiSamples();
                if (samples > 1 && renderer.getCaps().contains(Caps.OpenGL32)) {
                    assertTrue(actualSamples > 1, "desktop regression must exercise multisample textures");
                }
                verifySampler(renderer.getCaps().contains(Caps.OpenGLES20), actualSamples);
            }

            @Override
            public void simpleRender(RenderManager manager) {
                if (++frames != 5) {
                    return;
                }
                ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
                try {
                    renderer.readFrameBuffer(output, pixels);
                    byte[] data = new byte[pixels.remaining()];
                    pixels.get(data);
                    result.complete(data);
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    BufferUtils.destroyDirectBuffer(pixels);
                }
            }

            @Override
            public void handleError(String message, Throwable failure) {
                renderThread.compareAndSet(null, Thread.currentThread());
                result.completeExceptionally(failure == null ? new AssertionError(message) : failure);
            }
        };
        AppSettings settings = new AppSettings(true);
        settings.setResolution(SIZE, SIZE);
        settings.setAudioRenderer(null);
        settings.setUseInput(false);
        settings.setSamples(0);
        settings.setGammaCorrection(false);
        settings.setRenderer(System.getProperty("jme.screenshot.renderer", AppSettings.LWJGL_OPENGL32));
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setDisplayStatView(false);
        app.setDisplayFps(false);
        // Also stop on the render thread (macOS runs app.start synchronously).
        result.orTimeout(30, TimeUnit.SECONDS).whenComplete((pixels, failure) -> app.stop(false));
        try {
            app.start();
            return result.get(30, TimeUnit.SECONDS);
        } finally {
            app.stop(false);
            Thread thread = renderThread.get();
            if (thread != null && thread != Thread.currentThread()) {
                thread.join(TimeUnit.SECONDS.toMillis(10));
                assertFalse(thread.isAlive(), "native context did not finish shutting down");
            }
        }
    }
}
