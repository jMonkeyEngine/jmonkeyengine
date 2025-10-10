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
package org.jmonkeyengine.screenshottests.animation;

import com.jme3.anim.SkinningControl;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.GlVertexBuffer;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

/**
 * Screenshot test for JMonkeyEngine issue #2076: software skinning requires vertex
 * normals.
 *
 * <p>If the issue is resolved, 2 copies of the Jaime model will be rendered in the screenshot.
 *
 * <p>If the issue is present, then the application will immediately crash,
 * typically with a {@code NullPointerException}.
 *
 * @author Stephen Gold (original test)
 * @author Richard Tingle (screenshot test adaptation)
 */
public class TestIssue2076 extends ScreenshotTestBase {

    /**
     * This test creates a scene with two Jaime models, one using the old animation system
     * and one using the new animation system, both with software skinning and no vertex normals.
     */
    @Test
    public void testIssue2076() {
        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();
                AssetManager assetManager = simpleApplication.getAssetManager();

                // Add ambient light
                AmbientLight ambientLight = new AmbientLight();
                ambientLight.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
                rootNode.addLight(ambientLight);

                /*
                 * The original Jaime model was chosen for testing because it includes
                 * tangent buffers (needed to trigger issue #2076) and uses the old
                 * animation system (so it can be easily used to test both systems).
                 */
                String assetPath = "Models/Jaime/Jaime.j3o";

                // Test old animation system
                Node oldJaime = (Node) assetManager.loadModel(assetPath);
                rootNode.attachChild(oldJaime);
                oldJaime.setLocalTranslation(-1f, 0f, 0f);

                // Enable software skinning
                SkeletonControl skeletonControl = oldJaime.getControl(SkeletonControl.class);
                skeletonControl.setHardwareSkinningPreferred(false);

                // Remove its vertex normals
                Geometry oldGeometry = (Geometry) oldJaime.getChild(0);
                Mesh oldMesh = oldGeometry.getMesh();
                oldMesh.clearBuffer(GlVertexBuffer.Type.Normal);
                oldMesh.clearBuffer(GlVertexBuffer.Type.BindPoseNormal);

                // Test new animation system
                Node newJaime = (Node) assetManager.loadModel(assetPath);
                AnimMigrationUtils.migrate(newJaime);
                rootNode.attachChild(newJaime);
                newJaime.setLocalTranslation(1f, 0f, 0f);

                // Enable software skinning
                SkinningControl skinningControl = newJaime.getControl(SkinningControl.class);
                skinningControl.setHardwareSkinningPreferred(false);

                // Remove its vertex normals
                Geometry newGeometry = (Geometry) newJaime.getChild(0);
                Mesh newMesh = newGeometry.getMesh();
                newMesh.clearBuffer(GlVertexBuffer.Type.Normal);
                newMesh.clearBuffer(GlVertexBuffer.Type.BindPoseNormal);

                // Position the camera to see both models
                simpleApplication.getCamera().setLocation(new Vector3f(0f, 0f, 5f));
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
            }
        }).run();
    }
}