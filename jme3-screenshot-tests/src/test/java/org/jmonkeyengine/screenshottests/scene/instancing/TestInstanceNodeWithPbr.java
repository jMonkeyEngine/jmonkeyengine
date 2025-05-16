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
package org.jmonkeyengine.screenshottests.scene.instancing;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * This test specifically validates the corrected PBR rendering when combined
 * with instancing, as addressed in issue #2435.
 *
 * <p>
 * It creates an InstancedNode with a PBR-materialized Box to ensure the fix in 
 * PBRLighting.vert correctly handles world position calculations for instanced geometry.
 * </p>
 *
 * @author Ryan McDonough - original test
 * @author Richard Tingle (aka richtea) - screenshot test adaptation
 */
public class TestInstanceNodeWithPbr extends ScreenshotTestBase {

    @Test
    public void testInstanceNodeWithPbr() {
        screenshotTest(
            new BaseAppState() {
                private Geometry box;
                private float pos = -5;
                private float vel = 50;
                private BitmapText bmp;

                @Override
                protected void initialize(Application app) {
                    SimpleApplication simpleApp = (SimpleApplication) app;

                    app.getCamera().setLocation(Vector3f.UNIT_XYZ.mult(12));
                    app.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

                    bmp = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
                    bmp.setText("<placeholder>");
                    bmp.setLocalTranslation(10, app.getContext().getSettings().getHeight() - 20, 0);
                    bmp.setColor(ColorRGBA.Red);
                    simpleApp.getGuiNode().attachChild(bmp);

                    InstancedNode instancedNode = new InstancedNode("InstancedNode");
                    simpleApp.getRootNode().attachChild(instancedNode);

                    Box mesh = new Box(0.5f, 0.5f, 0.5f);
                    box = new Geometry("Box", mesh);
                    Material pbrMaterial = createPbrMaterial(app, ColorRGBA.Red);
                    box.setMaterial(pbrMaterial);

                    instancedNode.attachChild(box);
                    instancedNode.instance();

                    DirectionalLight light = new DirectionalLight();
                    light.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
                    simpleApp.getRootNode().addLight(light);
                }

                private Material createPbrMaterial(Application app, ColorRGBA color) {
                    Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Light/PBRLighting.j3md");
                    mat.setColor("BaseColor", color);
                    mat.setFloat("Roughness", 0.8f);
                    mat.setFloat("Metallic", 0.1f);
                    mat.setBoolean("UseInstancing", true);
                    return mat;
                }

                @Override
                public void update(float tpf) {
                    pos += tpf * vel;
                    box.setLocalTranslation(pos, 0f, 0f);

                    bmp.setText(String.format(Locale.ENGLISH, "BoxPosition: (%.2f, %.1f, %.1f)", pos, 0f, 0f));
                }

                @Override
                protected void cleanup(Application app) {}

                @Override
                protected void onEnable() {}

                @Override
                protected void onDisable() { }
            }
        )
        .setFramesToTakeScreenshotsOn(1, 10)
        .run();
    }
}