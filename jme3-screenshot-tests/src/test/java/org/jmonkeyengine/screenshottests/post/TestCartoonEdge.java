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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Texture;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for the CartoonEdge filter.
 * 
 * <p>This test creates a scene with a monkey head model that has a cartoon/cel-shaded effect
 * applied to it. The CartoonEdgeFilter is used to create yellow outlines around the edges
 * of the model, and a toon shader is applied to the model's material to create the cel-shaded look.
 * 
 * @author Richard Tingle (screenshot test adaptation)
 */
public class TestCartoonEdge extends ScreenshotTestBase {

    /**
     * This test creates a scene with a cartoon-shaded monkey head model.
     */
    @Test
    public void testCartoonEdge() {
        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();

                simpleApplication.getViewPort().setBackgroundColor(ColorRGBA.Gray);

                simpleApplication.getCamera().setLocation(new Vector3f(-1, 2, -5));
                simpleApplication.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                simpleApplication.getCamera().setFrustumFar(300);

                rootNode.setCullHint(CullHint.Never);

                setupLighting(rootNode);
                
                setupModel(simpleApplication, rootNode);
                
                setupFilters(simpleApplication);
            }

            private void setupFilters(SimpleApplication app) {
                if (app.getRenderer().getCaps().contains(Caps.GLSL100)) {
                    FilterPostProcessor fpp = new FilterPostProcessor(app.getAssetManager());

                    CartoonEdgeFilter toon = new CartoonEdgeFilter();
                    toon.setEdgeColor(ColorRGBA.Yellow);
                    fpp.addFilter(toon);
                    app.getViewPort().addProcessor(fpp);
                }
            }

            private void setupLighting(Node rootNode) {
                DirectionalLight dl = new DirectionalLight();
                dl.setDirection(new Vector3f(-1, -1, 1).normalizeLocal());
                dl.setColor(new ColorRGBA(2, 2, 2, 1));
                rootNode.addLight(dl);
            }

            private void setupModel(SimpleApplication app, Node rootNode) {
                Spatial model = app.getAssetManager().loadModel("Models/MonkeyHead/MonkeyHead.mesh.xml");
                makeToonish(app, model);
                model.rotate(0, FastMath.PI, 0);
                rootNode.attachChild(model);
            }

            private void makeToonish(SimpleApplication app, Spatial spatial) {
                if (spatial instanceof Node) {
                    Node n = (Node) spatial;
                    for (Spatial child : n.getChildren()) {
                        makeToonish(app, child);
                    }
                } else if (spatial instanceof Geometry) {
                    Geometry g = (Geometry) spatial;
                    Material m = g.getMaterial();
                    if (m.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                        Texture t = app.getAssetManager().loadTexture("Textures/ColorRamp/toon.png");
                        m.setTexture("ColorRamp", t);
                        m.setBoolean("UseMaterialColors", true);
                        m.setColor("Specular", ColorRGBA.Black);
                        m.setColor("Diffuse", ColorRGBA.White);
                        m.setBoolean("VertexLighting", true);
                    }
                }
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