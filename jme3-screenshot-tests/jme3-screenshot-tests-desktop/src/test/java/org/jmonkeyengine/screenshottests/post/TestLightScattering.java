/*
 * Copyright (c) 2025 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.post;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for the LightScattering filter.
 * 
 * <p>This test creates a scene with a terrain model and a sky, with a light scattering
 * (god rays) effect applied. The effect simulates light rays scattering through the atmosphere
 * from a bright light source (like the sun).
 * 
 * @author Richard Tingle (screenshot test adaptation)
 */
public class TestLightScattering extends ScreenshotTestBase {

    /**
     * This test creates a scene with a light scattering effect.
     */
    @Test
    public void testLightScattering() {
        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();

                simpleApplication.getCamera().setLocation(new Vector3f(55.35316f, -0.27061665f, 27.092093f));
                simpleApplication.getCamera().setRotation(new Quaternion(0.010414706f, 0.9874893f, 0.13880467f, -0.07409228f));

                Material mat = simpleApplication.getAssetManager().loadMaterial("Textures/Terrain/Rocky/Rocky.j3m");
                Spatial scene = simpleApplication.getAssetManager().loadModel("Models/Terrain/Terrain.mesh.xml");
                MikktspaceTangentGenerator.generate(((Geometry) ((Node) scene).getChild(0)).getMesh());
                scene.setMaterial(mat);
                scene.setShadowMode(ShadowMode.CastAndReceive);
                scene.setLocalScale(400);
                scene.setLocalTranslation(0, -10, -120);
                rootNode.attachChild(scene);

                rootNode.attachChild(SkyFactory.createSky(simpleApplication.getAssetManager(),
                        "Textures/Sky/Bright/FullskiesBlueClear03.dds", 
                        SkyFactory.EnvMapType.CubeMap));

                DirectionalLight sun = new DirectionalLight();
                Vector3f lightDir = new Vector3f(-0.12f, -0.3729129f, 0.74847335f);
                sun.setDirection(lightDir);
                sun.setColor(ColorRGBA.White.clone().multLocal(2));
                scene.addLight(sun);

                FilterPostProcessor fpp = new FilterPostProcessor(simpleApplication.getAssetManager());

                Vector3f lightPos = lightDir.normalize().negate().multLocal(3000);
                LightScatteringFilter filter = new LightScatteringFilter(lightPos);

                filter.setLightDensity(1.0f);
                filter.setBlurStart(0.02f);
                filter.setBlurWidth(0.9f);
                filter.setLightPosition(lightPos);
                
                fpp.addFilter(filter);
                simpleApplication.getViewPort().addProcessor(fpp);
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
        })
        .setFramesToTakeScreenshotsOn(1)
        .run();
    }
}