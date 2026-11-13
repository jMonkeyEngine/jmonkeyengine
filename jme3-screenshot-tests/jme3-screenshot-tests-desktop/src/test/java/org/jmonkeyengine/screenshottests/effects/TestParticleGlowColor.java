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
package org.jmonkeyengine.screenshottests.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
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

/** Regression coverage for particle vertex color in the glow pass (issue #1030). */
@Tag("integration")
public class TestParticleGlowColor {

    private static final int SIZE = 16;

    @Test
    public void glowTechniqueUsesParticleVertexColor() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        CountDownLatch finished = new CountDownLatch(1);
        TestContainingApp app = new TestContainingApp() {
            @Override
            public void simpleInitApp() {
                try {
                    verifyGlowColor(renderManager, assetManager);
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
        settings.setResolution(SIZE, SIZE);
        settings.setAudioRenderer(null);
        settings.setUseInput(false);
        settings.setRenderer(System.getProperty("jme.screenshot.renderer", AppSettings.LWJGL_OPENGL45));
        app.setSettings(settings);
        app.setShowSettings(false);
        new DesktopRunner().runApplicationUntilScenarioCompletes(app, finished);
        result.get(1, TimeUnit.SECONDS);
    }

    private static void verifyGlowColor(RenderManager manager, AssetManager assets) {
        Material particleMaterial = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        assertTrue(particleMaterial.getMaterialDef().getTechniqueDefs("Glow").stream()
                .anyMatch(def -> "GLSL120".equals(def.getVertexShaderLanguage())
                        && "GLSL120".equals(def.getFragmentShaderLanguage())),
                "Glow technique must provide GLSL120 for desktop point sprites");

        Renderer renderer = manager.getRenderer();
        renderer.setMainFrameBufferSrgb(false);
        renderer.setLinearizeSrgbImages(false);

        Camera camera = new Camera(SIZE, SIZE);
        camera.setParallelProjection(true);
        camera.setFrustum(0.1f, 10, -1, 1, 1, -1);
        camera.setLocation(new Vector3f(0, 0, 2));
        camera.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        manager.setCamera(camera, false);

        FrameBuffer target = new FrameBuffer(SIZE, SIZE, 1);
        target.setColorBuffer(Image.Format.RGBA8);
        target.setDepthBuffer(Image.Format.Depth24);
        try {
            renderer.setFrameBuffer(target);
            Geometry geometry = new Geometry("particle-color", new Quad(2, 2));
            geometry.move(-1, -1, 0);
            geometry.getMesh().setBuffer(VertexBuffer.Type.Color, 4, new float[]{
                    0.2f, 0.4f, 0.6f, 1f,
                    0.2f, 0.4f, 0.6f, 1f,
                    0.2f, 0.4f, 0.6f, 1f,
                    0.2f, 0.4f, 0.6f, 1f
            });
            geometry.updateGeometricState();
            Texture2D whiteMap = whiteTexture();

            assertGlowPixel(manager, renderer, target, assets, geometry, whiteMap,
                    new ColorRGBA(0.8f, 0.5f, 0.25f, 1f), 41, 51, 38,
                    "glow map and glow color");
            assertGlowPixel(manager, renderer, target, assets, geometry, whiteMap,
                    null, 51, 102, 153, "glow map only");
            assertGlowPixel(manager, renderer, target, assets, geometry, null,
                    new ColorRGBA(0.5f, 0.5f, 0.5f, 1f), 26, 51, 77,
                    "glow color only");
            assertGlowPixel(manager, renderer, target, assets, geometry, null,
                    null, 0, 0, 0, "no glow parameters");
            assertPointSpriteGlowPixel(manager, renderer, target, assets, whiteMap);
        } finally {
            renderer.setFrameBuffer(null);
            target.dispose();
        }
    }

    private static void assertGlowPixel(RenderManager manager, Renderer renderer, FrameBuffer target,
            AssetManager assets, Geometry geometry, Texture2D glowMap, ColorRGBA glowColor,
            int expectedRed, int expectedGreen, int expectedBlue, String variant) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        if (glowMap != null) {
            material.setTexture("GlowMap", glowMap);
        }
        if (glowColor != null) {
            material.setColor("GlowColor", glowColor);
        }
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        material.selectTechnique("Glow", manager);
        geometry.setMaterial(material);

        renderer.setBackgroundColor(ColorRGBA.Black);
        renderer.clearBuffers(true, true, true);
        manager.renderGeometry(geometry);

        ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        try {
            renderer.readFrameBufferWithFormat(target, pixels, Image.Format.RGBA8);
            int center = (SIZE / 2 * SIZE + SIZE / 2) * 4;
            assertEquals(expectedRed, pixels.get(center) & 255, 2, variant + " red channel");
            assertEquals(expectedGreen, pixels.get(center + 1) & 255, 2, variant + " green channel");
            assertEquals(expectedBlue, pixels.get(center + 2) & 255, 2, variant + " blue channel");
        } finally {
            BufferUtils.destroyDirectBuffer(pixels);
        }
    }

    private static void assertPointSpriteGlowPixel(RenderManager manager, Renderer renderer,
            FrameBuffer target, AssetManager assets, Texture2D glowMap) {
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{0, 0, 0});
        mesh.setBuffer(VertexBuffer.Type.Color, 4, new float[]{0.2f, 0.4f, 0.6f, 1f});
        mesh.setBuffer(VertexBuffer.Type.Size, 1, new float[]{4f});
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 4, new float[]{0, 0, 1, 1});
        mesh.updateBound();
        mesh.updateCounts();

        Material material = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        material.setTexture("GlowMap", glowMap);
        material.setColor("GlowColor", ColorRGBA.White);
        material.setBoolean("PointSprite", true);
        material.setFloat("Quadratic", 1f);
        material.selectTechnique("Glow", manager);

        Geometry geometry = new Geometry("point-particle-color", mesh);
        geometry.setMaterial(material);
        renderer.setBackgroundColor(ColorRGBA.Black);
        renderer.clearBuffers(true, true, true);
        manager.renderGeometry(geometry);

        ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        try {
            renderer.readFrameBufferWithFormat(target, pixels, Image.Format.RGBA8);
            int center = (SIZE / 2 * SIZE + SIZE / 2) * 4;
            assertEquals(51, pixels.get(center) & 255, 2, "point sprite red channel");
            assertEquals(102, pixels.get(center + 1) & 255, 2, "point sprite green channel");
            assertEquals(153, pixels.get(center + 2) & 255, 2, "point sprite blue channel");
        } finally {
            BufferUtils.destroyDirectBuffer(pixels);
        }
    }

    private static Texture2D whiteTexture() {
        ByteBuffer data = BufferUtils.createByteBuffer(4);
        data.put(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255}).flip();
        Texture2D texture = new Texture2D(new Image(Image.Format.RGBA8, 1, 1, data, ColorSpace.Linear));
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        return texture;
    }
}
