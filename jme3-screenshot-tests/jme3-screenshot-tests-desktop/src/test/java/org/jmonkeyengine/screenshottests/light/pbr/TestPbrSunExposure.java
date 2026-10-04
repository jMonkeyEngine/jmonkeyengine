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
package org.jmonkeyengine.screenshottests.light.pbr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jmonkeyengine.screenshottests.testframework.TestContainingApp;
import org.jmonkeyengine.screenshottests.testframework.desktop.DesktopRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Compiles and renders exposure variants on both desktop and ANGLE CI jobs. */
@Tag("integration")
public class TestPbrSunExposure {

    @Test
    public void sunlightInputsRenderExpectedExposure() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        CountDownLatch finished = new CountDownLatch(1);
        final TestContainingApp app = new TestContainingApp() {
            @Override
            public void simpleInitApp() {
                try {
                    verifyExposure(renderManager, assetManager);
                    result.complete(null);
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    finished.countDown();
                }
            }

            @Override
            public void handleError(String message, Throwable failure) {
                result.completeExceptionally(failure == null ? new AssertionError(message) : failure);
                stop();
                finished.countDown();
            }
        };
        AppSettings settings = new AppSettings(true);
        settings.setResolution(16, 16);
        settings.setAudioRenderer(null);
        settings.setUseInput(false);
        settings.setRenderer(System.getProperty("jme.screenshot.renderer", AppSettings.LWJGL_OPENGL45));
        app.setSettings(settings);
        app.setShowSettings(false);
        new DesktopRunner().runApplicationUntilScenarioCompletes(app, finished);
        result.get(1, TimeUnit.SECONDS);
    }

    static void verifyExposure(RenderManager manager, AssetManager assets) {
        Renderer renderer = manager.getRenderer();
        renderer.setMainFrameBufferSrgb(false);
        renderer.setLinearizeSrgbImages(false);
        Camera camera = new Camera(16, 16);
        camera.setParallelProjection(true);
        camera.setFrustum(0.1f, 10, -1, 1, 1, -1);
        camera.setLocation(new Vector3f(0, 0, 2));
        camera.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        manager.setCamera(camera, false);
        FrameBuffer target = new FrameBuffer(16, 16, 1);
        target.setColorBuffer(Image.Format.RGBA8);
        target.setDepthBuffer(Image.Format.Depth24);
        try {
            renderer.setFrameBuffer(target);
            for (String variant : new String[]{"static", "vertex", "map", "parallax", "no-tangents",
                    "module-map", "module-control"}) {
                boolean module = variant.startsWith("module");
                Material material = new Material(assets, module
                        ? "TestMatDefs/PbrSunExposureModule.j3md" : "Common/MatDefs/Light/PBRLighting.j3md");
                if (!module) {
                    material.setInt("DebugValuesMode", 6);
                    material.setBoolean("UseSpecularAA", false);
                }
                material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                Geometry geometry = new Geometry("exposure", new Quad(2, 2));
                geometry.move(-1, -1, 0);
                geometry.setMaterial(material);
                if (variant.equals("static")) {
                    material.setFloat("StaticSunIntensity", 0.25f);
                } else if (variant.equals("vertex")) {
                    material.setBoolean("UseVertexColorsAsSunIntensity", true);
                    geometry.getMesh().setBuffer(VertexBuffer.Type.Color, 4, new float[]{
                            0.25f, 0, 0, 1, 0.25f, 0, 0, 1, 0.25f, 0, 0, 1, 0.25f, 0, 0, 1});
                } else if (!variant.equals("module-control")) {
                    material.setTexture("SunLightExposureMap", exposureMap());
                }
                if (variant.equals("parallax") || variant.equals("no-tangents")) {
                    material.setTexture("ParallaxMap", exposureMap());
                    material.setFloat("ParallaxHeight", 0);
                    if (variant.equals("parallax")) {
                        geometry.getMesh().setBuffer(VertexBuffer.Type.Tangent, 4, new float[]{
                                1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1});
                    }
                }
                renderer.setBackgroundColor(ColorRGBA.Black);
                renderer.clearBuffers(true, true, true);
                geometry.updateGeometricState();
                manager.renderGeometry(geometry);
                ByteBuffer pixels = BufferUtils.createByteBuffer(16 * 16 * 4);
                renderer.readFrameBufferWithFormat(target, pixels, Image.Format.RGBA8);
                int left = variant.equals("module-control") ? 255 : 64;
                int right = variant.equals("static") || variant.equals("vertex") ? 64
                        : variant.equals("module-control") ? 255 : 192;
                assertPixel(pixels, 4, left, variant);
                assertPixel(pixels, 12, right, variant);
            }
        } finally {
            renderer.setFrameBuffer(null);
            target.dispose();
        }
    }

    private static Texture2D exposureMap() {
        ByteBuffer data = BufferUtils.createByteBuffer(8);
        data.put(new byte[]{64, 64, 64, (byte) 255, (byte) 192, (byte) 192, (byte) 192, (byte) 255}).flip();
        Texture2D texture = new Texture2D(new Image(Image.Format.RGBA8, 2, 1, data, ColorSpace.Linear));
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        return texture;
    }

    private static void assertPixel(ByteBuffer pixels, int x, int expected, String variant) {
        int offset = (8 * 16 + x) * 4;
        for (int channel = 0; channel < 3; channel++) {
            assertEquals(expected, pixels.get(offset + channel) & 255, 1, variant + " channel " + channel);
        }
        assertEquals(255, pixels.get(offset + 3) & 255, variant + " alpha");
    }
}
