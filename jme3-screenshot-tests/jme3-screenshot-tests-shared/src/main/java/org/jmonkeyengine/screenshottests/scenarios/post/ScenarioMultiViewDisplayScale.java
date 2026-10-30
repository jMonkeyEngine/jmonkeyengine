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
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jmonkeyengine.screenshottests.scenarios.post;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;
import org.jmonkeyengine.screenshottests.testframework.TestResolution;

/**
 * Split-screen FPP scenario rendered with a deterministic 2x display scale.
 */
public final class ScenarioMultiViewDisplayScale {

    private static final int LOGICAL_WIDTH = 320;
    private static final int LOGICAL_HEIGHT = 240;
    private static final float DISPLAY_SCALE = 2f;

    private ScenarioMultiViewDisplayScale() {
    }

    public static ScreenshotTest testMultiViewDisplayScale() {
        return new ScreenshotTest(new BaseAppState() {
            private ViewPort rightView;
            private FilterPostProcessor leftProcessor;
            private FilterPostProcessor rightProcessor;

            @Override
            protected void initialize(Application application) {
                SimpleApplication app = (SimpleApplication) application;

                Camera leftCamera = app.getCamera();
                leftCamera.setViewPort(0f, 0.5f, 0f, 1f);
                leftCamera.setFrustumPerspective(45f, 2f / 3f, 1f, 100f);
                leftCamera.setLocation(new Vector3f(0f, 0f, 8f));
                leftCamera.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

                Camera rightCamera = leftCamera.clone();
                rightCamera.setViewPort(0.5f, 1f, 0f, 1f);

                ViewPort leftView = app.getViewPort();
                leftView.setBackgroundColor(new ColorRGBA(0.04f, 0.04f, 0.08f, 1f));
                leftView.setClearFlags(true, true, true);

                rightView = app.getRenderManager().createMainView("right", rightCamera);
                rightView.setBackgroundColor(new ColorRGBA(0.04f, 0.04f, 0.08f, 1f));
                rightView.setClearFlags(true, true, true);
                rightView.attachScene(app.getRootNode());

                attachGeometry(app);

                leftProcessor = new FilterPostProcessor(app.getAssetManager());
                leftProcessor.addFilter(new ColorOverlayFilter(
                        new ColorRGBA(1f, 0.25f, 0.25f, 1f)));
                leftView.addProcessor(leftProcessor);

                rightProcessor = new FilterPostProcessor(app.getAssetManager());
                rightProcessor.addFilter(new ColorOverlayFilter(
                        new ColorRGBA(0.25f, 0.7f, 1f, 1f)));
                rightView.addProcessor(rightProcessor);
            }

            private void attachGeometry(SimpleApplication app) {
                Material white = new Material(app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md");
                white.setColor("Color", ColorRGBA.White);

                Geometry center = new Geometry("center", new Box(1.25f, 1.25f, 1.25f));
                center.setMaterial(white);
                center.rotate(0.35f, 0.55f, 0.1f);
                app.getRootNode().attachChild(center);

                Geometry marker = new Geometry("upper marker", new Box(0.35f, 0.35f, 0.35f));
                marker.setMaterial(white.clone());
                marker.setLocalTranslation(0.9f, 1.8f, 0f);
                app.getRootNode().attachChild(marker);
            }

            @Override
            protected void cleanup(Application application) {
                SimpleApplication app = (SimpleApplication) application;
                if (leftProcessor != null) {
                    app.getViewPort().removeProcessor(leftProcessor);
                }
                if (rightView != null) {
                    if (rightProcessor != null) {
                        rightView.removeProcessor(rightProcessor);
                    }
                    app.getRenderManager().removeMainView(rightView);
                }
            }

            @Override
            protected void onEnable() {
            }

            @Override
            protected void onDisable() {
            }
        })
                .setTestResolution(new TestResolution(LOGICAL_WIDTH, LOGICAL_HEIGHT))
                .setDisplayScaleMode(DISPLAY_SCALE)
                .setFramesToTakeScreenshotsOn(2);
    }
}
