package org.jmonkeyengine.screenshottests.scenarios.animation;

import static org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase.screenshotTest;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.AmbientLight;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;


import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

public class ScenarioIssue2076 {

    /**
     * This test creates a scene with two Jaime models, one using the old animation system
     * and one using the new animation system, both with software skinning and no vertex normals.
     */
    public static ScreenshotTest testIssue2076(){
        return screenshotTest(new BaseAppState() {
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
                oldMesh.clearBuffer(VertexBuffer.Type.Normal);
                oldMesh.clearBuffer(VertexBuffer.Type.BindPoseNormal);

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
                newMesh.clearBuffer(VertexBuffer.Type.Normal);
                newMesh.clearBuffer(VertexBuffer.Type.BindPoseNormal);

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
        });
    }

}
