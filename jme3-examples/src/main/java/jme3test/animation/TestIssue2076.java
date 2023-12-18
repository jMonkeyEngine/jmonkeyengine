/*
 * Copyright (c) 2023 jMonkeyEngine
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
package jme3test.animation;

import com.jme3.anim.SkinningControl;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;

/**
 * Test for JMonkeyEngine issue #2076: software skinning requires vertex
 * normals.
 *
 * <p>If the issue is resolved, 2 copies of the Jaime model will be rendered.
 *
 * <p>If the issue is present, then the application will immediately crash,
 * typically with a {@code NullPointerException}.
 *
 * @author Stephen Gold
 */
public class TestIssue2076 extends SimpleApplication {
    /**
     * Main entry point for the TestIssue2076 application.
     *
     * @param args array of command-line arguments (not null)
     */
    public static void main(String... args) {
        TestIssue2076 app = new TestIssue2076();
        app.start();
    }

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
        rootNode.addLight(ambientLight);
        /*
         * The original Jaime model was chosen for testing because it includes
         * tangent buffers (needed to trigger issue #2076) and uses the old
         * animation system (so it can be easily used to test both systems).
         */
        String assetPath = "Models/Jaime/Jaime.j3o";

        testOldAnimationSystem(assetPath);
        testNewAnimationSystem(assetPath);
    }

    /**
     * Attach the specified model (which uses the old animation system) to the
     * scene graph, enable software skinning, and remove its vertex normals.
     *
     * @param assetPath the asset path of the test model (not null)
     */
    private void testOldAnimationSystem(String assetPath) {
        Node oldJaime = (Node) assetManager.loadModel(assetPath);
        rootNode.attachChild(oldJaime);
        oldJaime.setLocalTranslation(-1f, 0f, 0f);

        // enable software skinning:
        SkeletonControl skeletonControl
                = oldJaime.getControl(SkeletonControl.class);
        skeletonControl.setHardwareSkinningPreferred(false);

        // remove its vertex normals:
        Geometry oldGeometry = (Geometry) oldJaime.getChild(0);
        Mesh oldMesh = oldGeometry.getMesh();
        oldMesh.clearBuffer(VertexBuffer.Type.Normal);
        oldMesh.clearBuffer(VertexBuffer.Type.BindPoseNormal);
    }

    /**
     * Attach the specified model, converted to the new animation system, to the
     * scene graph, enable software skinning, and remove its vertex normals.
     *
     * @param assetPath the asset path of the test model (not null)
     */
    private void testNewAnimationSystem(String assetPath) {
        Node newJaime = (Node) assetManager.loadModel(assetPath);
        AnimMigrationUtils.migrate(newJaime);
        rootNode.attachChild(newJaime);
        newJaime.setLocalTranslation(1f, 0f, 0f);

        // enable software skinning:
        SkinningControl skinningControl
                = newJaime.getControl(SkinningControl.class);
        skinningControl.setHardwareSkinningPreferred(false);

        // remove its vertex normals:
        Geometry newGeometry = (Geometry) newJaime.getChild(0);
        Mesh newMesh = newGeometry.getMesh();
        newMesh.clearBuffer(VertexBuffer.Type.Normal);
        newMesh.clearBuffer(VertexBuffer.Type.BindPoseNormal);
    }
}
