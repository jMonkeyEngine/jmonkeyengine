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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jmonkeyengine.screenshottests.scenarios.light.pbr;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

/**
 * Deterministic sunlight-exposure charts using the production PBR material.
 * Each chart has exposure debug output (mode 6) above actual sunlit PBR output.
 * No external assets, probes, shadows, post-processing or asynchronous baking
 * are needed, so the same scene can run on desktop OpenGL, ANGLE and Android.
 */
public final class ScenarioPBRSunlight {

    private enum Input {
        DEFAULT, ZERO, STATIC, VERTEX, MAP, COMBINED
    }

    private ScenarioPBRSunlight() {
    }

    /**
     * Columns, left to right: no exposure parameters, static zero, static 0.5,
     * vertex red, map red, and all three inputs multiplied together.
     *
     * <p>The vertex red channel rises from 0.25 to 0.75, while the map's four
     * equal-width bands contain 0, 64/255, 128/255 and 1. The combined column is
     * {@code 0.5 * (0.25 + 0.5 * u) * mapRed(u)}. Other vertex/texture channels
     * deliberately differ from red. Vertex colours must not tint the albedo.
     * The first debug column must be white and the second black; their lit
     * counterparts must be fully lit and black respectively.</p>
     *
     * @return the screenshot test configuration
     */
    public static ScreenshotTest testSunlightInputs() {
        return screenshotTest(new SunlightChart(false)).setFramesToTakeScreenshotsOn(3);
    }

    /**
     * Columns, left to right: exposure map without parallax, with parallax and
     * valid tangents, and with parallax but no tangent buffer.
     *
     * <p>All quads are tilted 45 degrees. A constant maximum-height map and a
     * deliberately large nonzero height shift the middle column's exposure
     * bands to the right. The outer debug columns must retain identical band
     * widths. This detects both sampling the original UVs after parallax and
     * attempting a parallax offset without a valid tangent basis.</p>
     *
     * @return the screenshot test configuration
     */
    public static ScreenshotTest testSunlightParallax() {
        return screenshotTest(new SunlightChart(true)).setFramesToTakeScreenshotsOn(3);
    }

    private static final class SunlightChart extends BaseAppState {
        private final boolean parallax;
        private final Node chart = new Node("sunlight exposure chart");
        private final int columns;

        private SunlightChart(boolean parallax) {
            this.parallax = parallax;
            columns = parallax ? 3 : Input.values().length;
        }

        @Override
        protected void initialize(Application app) {
            Camera camera = app.getCamera();
            camera.setParallelProjection(true);
            camera.setLocation(new Vector3f(0, 0, 30));
            camera.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
            updateFrustum(camera);

            // Keep debug values linear, including the procedural exposure map.
            app.getRenderer().setMainFrameBufferSrgb(false);
            app.getRenderer().setLinearizeSrgbImages(false);
            app.getViewPort().setBackgroundColor(new ColorRGBA(0.06f, 0.08f, 0.12f, 1));

            DirectionalLight sunlight = new DirectionalLight();
            sunlight.setDirection(new Vector3f(0, 0, -1));
            sunlight.setColor(new ColorRGBA(1.5f, 1.5f, 1.5f, 1));
            chart.addLight(sunlight);
            ((SimpleApplication) app).getRootNode().attachChild(chart);

            Texture2D exposureMap = redChannelMap(0, 64, 128, 255);
            Texture2D heightMap = redChannelMap(255);
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < columns; column++) {
                    Geometry tile = createTile(app.getAssetManager(), row == 0);
                    tile.setLocalTranslation(2 * column - columns + 1, 1 - 2 * row, 0);
                    if (parallax) {
                        configureParallax(tile, column, exposureMap, heightMap);
                    } else {
                        configureInput(tile, Input.values()[column], exposureMap);
                    }
                    chart.attachChild(tile);
                }
            }
        }

        @Override
        public void update(float tpf) {
            // Android surface resizes can otherwise replace the horizontal
            // frustum. Preserve the complete chart at every capture resolution.
            updateFrustum(getApplication().getCamera());
        }

        private void updateFrustum(Camera camera) {
            camera.setFrustum(1, 100, -columns, columns, 2.4f, -2.4f);
        }

        @Override
        protected void cleanup(Application app) {
            chart.removeFromParent();
        }

        @Override
        protected void onEnable() {
        }

        @Override
        protected void onDisable() {
        }
    }

    private static Geometry createTile(AssetManager assets, boolean debug) {
        Quad mesh = new Quad(1.6f, 1.5f);
        // Centre the quad so that parallax columns have identical silhouettes.
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{
                -0.8f, -0.75f, 0, 0.8f, -0.75f, 0,
                0.8f, 0.75f, 0, -0.8f, 0.75f, 0
        });
        mesh.updateBound();
        // Every tile has these colours; only VERTEX and COMBINED enable them.
        mesh.setBuffer(VertexBuffer.Type.Color, 4, new float[]{
                0.25f, 1, 0, 1, 0.75f, 0, 1, 1,
                0.75f, 0, 1, 1, 0.25f, 1, 0, 1
        });

        Material material = new Material(assets, "Common/MatDefs/Light/PBRLighting.j3md");
        material.setColor("BaseColor", new ColorRGBA(0.8f, 0.35f, 0.12f, 1));
        material.setFloat("Metallic", 0);
        material.setFloat("Roughness", 0.8f);
        material.setBoolean("UseSpecularAA", false);
        if (debug) {
            material.setInt("DebugValuesMode", 6);
        }
        Geometry tile = new Geometry(debug ? "exposure" : "sunlit PBR", mesh);
        tile.setMaterial(material);
        return tile;
    }

    private static void configureInput(Geometry tile, Input input, Texture2D exposureMap) {
        Material material = tile.getMaterial();
        if (input == Input.ZERO) {
            material.setFloat("StaticSunIntensity", 0);
        } else if (input == Input.STATIC || input == Input.COMBINED) {
            material.setFloat("StaticSunIntensity", 0.5f);
        }
        if (input == Input.VERTEX || input == Input.COMBINED) {
            material.setBoolean("UseVertexColorsAsSunIntensity", true);
        }
        if (input == Input.MAP || input == Input.COMBINED) {
            material.setTexture("SunLightExposureMap", exposureMap);
        }
    }

    private static void configureParallax(Geometry tile, int column,
            Texture2D exposureMap, Texture2D heightMap) {
        tile.rotate(0, FastMath.QUARTER_PI, 0);
        Material material = tile.getMaterial();
        material.setTexture("SunLightExposureMap", exposureMap);
        if (column != 2) {
            tile.getMesh().setBuffer(VertexBuffer.Type.Tangent, 4, new float[]{
                    1, 0, 0, 1, 1, 0, 0, 1,
                    1, 0, 0, 1, 1, 0, 0, 1
            });
        }
        if (column != 0) {
            material.setTexture("ParallaxMap", heightMap);
            material.setFloat("ParallaxHeight", 0.75f);
            material.setBoolean("SteepParallax", false);
        }
    }

    private static Texture2D redChannelMap(int... redValues) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(redValues.length * 4);
        for (int red : redValues) {
            // Non-red channels are intentionally unsuitable as exposure data.
            pixels.put((byte) red).put((byte) (255 - red)).put((byte) 32).put((byte) 255);
        }
        pixels.flip();
        Texture2D texture = new Texture2D(new Image(Image.Format.RGBA8,
                redValues.length, 1, pixels, ColorSpace.Linear));
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        return texture;
    }
}
