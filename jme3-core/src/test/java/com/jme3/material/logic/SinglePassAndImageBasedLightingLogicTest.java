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
package com.jme3.material.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.Shader;
import com.jme3.system.NullRenderer;
import com.jme3.system.TestUtil;
import com.jme3.texture.TextureCubeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the per-draw marker used to avoid repeating PBR's non-direct lighting. */
public class SinglePassAndImageBasedLightingLogicTest {

    @Test
    public void noLightsStillRendersTheFirstPass() {
        Fixture fixture = new Fixture(1);
        fixture.render(0, false);
        fixture.assertPasses(Arrays.asList(true), Arrays.asList(0));
    }

    @Test
    public void ambientOnlyStillRendersTheFirstPass() {
        Fixture fixture = new Fixture(1);
        fixture.render(0, true);
        fixture.assertPasses(Arrays.asList(true), Arrays.asList(0));
    }

    @Test
    public void probesOnlyStillRenderTheFirstPass() {
        Fixture fixture = new Fixture(1);
        fixture.render(0, false, 3);
        fixture.assertPasses(Arrays.asList(true), Arrays.asList(0));
    }

    @Test
    public void probesDoNotChangeDirectLightBatching() {
        Fixture fixture = new Fixture(1);
        fixture.render(3, true, 3);
        fixture.assertPasses(Arrays.asList(true, false, false), Arrays.asList(1, 1, 1));
    }

    @Test
    public void oneBatchIsTheFirstPass() {
        Fixture fixture = new Fixture(3);
        fixture.render(3, false);
        fixture.assertPasses(Arrays.asList(true), Arrays.asList(3));
    }

    @Test
    public void additionalBatchesAreNotFirstPasses() {
        Fixture fixture = new Fixture(1);
        fixture.render(3, false);
        fixture.assertPasses(Arrays.asList(true, false, false), Arrays.asList(1, 1, 1));
    }

    @Test
    public void partialLastBatchPreservesAllDirectLights() {
        Fixture fixture = new Fixture(2);
        fixture.render(3, true);
        fixture.assertPasses(Arrays.asList(true, false), Arrays.asList(2, 1));
    }

    @Test
    public void firstPassMarkerResetsWhenTheShaderIsReused() {
        Fixture fixture = new Fixture(1);
        fixture.render(3, false);
        fixture.render(0, false);
        fixture.render(1, false);
        fixture.assertPasses(Arrays.asList(true, false, false, true, true),
                Arrays.asList(1, 1, 1, 0, 1));
    }

    private static class Fixture {
        final RecordingRenderer renderer = new RecordingRenderer();
        final RenderManager manager = new RenderManager(renderer);
        final Geometry geometry = new Geometry("quad", new Quad(1, 1));
        final Material material;

        Fixture(int batchSize) {
            AssetManager assets = TestUtil.createAssetManager();
            material = new Material(assets, "Common/MatDefs/Light/PBRLighting.j3md");
            material.setColor("Emissive", new ColorRGBA(0.1f, 0.2f, 0.3f, 1));
            geometry.setMaterial(material);
            geometry.updateGeometricState();
            manager.setSinglePassLightBatchSize(batchSize);
            manager.setCamera(new Camera(16, 16), false);
        }

        void render(int directLights, boolean ambientLight) {
            render(directLights, ambientLight, 0);
        }

        void render(int directLights, boolean ambientLight, int probes) {
            LightList lights = new LightList(geometry);
            if (ambientLight) {
                lights.add(new AmbientLight(ColorRGBA.White));
            }
            for (int i = 0; i < directLights; ++i) {
                lights.add(new DirectionalLight(new Vector3f(0, 0, -1), ColorRGBA.White));
            }
            for (int i = 0; i < probes; ++i) {
                LightProbe probe = new LightProbe();
                Vector3f[] coefficients = new Vector3f[9];
                for (int j = 0; j < coefficients.length; ++j) {
                    coefficients[j] = new Vector3f();
                }
                probe.setShCoeffs(coefficients);
                probe.setPrefilteredMap(new TextureCubeMap());
                lights.add(probe);
            }
            material.render(geometry, lights, manager);
        }

        void assertPasses(List<Boolean> firstPasses, List<Integer> lightCounts) {
            assertEquals(firstPasses, renderer.firstPasses);
            assertEquals(lightCounts, renderer.lightCounts);
        }
    }

    private static class RecordingRenderer extends NullRenderer {
        final List<Boolean> firstPasses = new ArrayList<>();
        final List<Integer> lightCounts = new ArrayList<>();
        Shader currentShader;

        @Override
        public EnumSet<Caps> getCaps() {
            return EnumSet.of(Caps.GLSL100, Caps.GLSL110, Caps.GLSL150);
        }

        @Override
        public void setShader(Shader shader) {
            currentShader = shader;
        }

        @Override
        public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
            firstPasses.add((Boolean) currentShader.getUniform("g_IsFirstLightPass").getValue());
            lightCounts.add((Integer) currentShader.getUniform("g_LightCount").getValue());
        }
    }
}
