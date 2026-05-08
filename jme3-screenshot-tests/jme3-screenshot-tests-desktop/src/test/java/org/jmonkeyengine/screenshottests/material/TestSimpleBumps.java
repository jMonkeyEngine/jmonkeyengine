/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.material;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for the SimpleBumps material test.
 * 
 * <p>This test creates a quad with a bump map material and a point light that orbits around it.
 * The light's position is represented by a small red sphere. Screenshots are taken at frames 10 and 60
 * to capture the light at different positions in its orbit.
 * 
 * @author Richard Tingle (screenshot test adaptation)
 */
public class TestSimpleBumps extends ScreenshotTestBase {

    /**
     * This test creates a scene with a bump-mapped quad and an orbiting light.
     */
    @Test
    public void testSimpleBumps() {
        screenshotTest(new BaseAppState() {
            private float angle;
            private PointLight pl;
            private Spatial lightMdl;

            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();
                AssetManager assetManager = simpleApplication.getAssetManager();

                // Create quad with bump map material
                Quad quadMesh = new Quad(1, 1);
                Geometry sphere = new Geometry("Rock Ball", quadMesh);
                Material mat = assetManager.loadMaterial("Textures/BumpMapTest/SimpleBump.j3m");
                sphere.setMaterial(mat);
                MikktspaceTangentGenerator.generate(sphere);
                rootNode.attachChild(sphere);

                // Create light representation
                lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
                lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
                rootNode.attachChild(lightMdl);

                // Create point light
                pl = new PointLight();
                pl.setColor(ColorRGBA.White);
                pl.setPosition(new Vector3f(0f, 0f, 4f));
                rootNode.addLight(pl);
            }

            @Override
            protected void cleanup(Application app) {
            }

            @Override
            protected void onEnable() {
            }

            @Override
            protected void onDisable() {
            }

            @Override
            public void update(float tpf) {
                super.update(tpf);

                angle += tpf * 2f;
                angle %= FastMath.TWO_PI;

                pl.setPosition(new Vector3f(FastMath.cos(angle) * 4f, 0.5f, FastMath.sin(angle) * 4f));
                lightMdl.setLocalTranslation(pl.getPosition());
            }
        })
        .setFramesToTakeScreenshotsOn(10, 60)
        .run();
    }
}