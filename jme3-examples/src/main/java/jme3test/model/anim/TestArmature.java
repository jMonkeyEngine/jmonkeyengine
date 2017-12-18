package jme3test.model.anim;

import com.jme3.animation.*;
import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.debug.custom.ArmatureDebugAppState;
import com.jme3.util.TangentBinormalGenerator;

/**
 * Created by Nehon on 18/12/2017.
 */
public class TestArmature extends SimpleApplication {

    Joint j1;
    Joint j2;

    public static void main(String... argv) {
        TestArmature app = new TestArmature();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        renderManager.setSinglePassLightBatchSize(2);
        //cam.setFrustumPerspective(90f, (float) cam.getWidth() / cam.getHeight(), 0.01f, 10f);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        Joint root = new Joint("Root_Joint");
        j1 = new Joint("Joint_1");
        j2 = new Joint("Joint_2");
        root.addChild(j1);
        j1.addChild(j2);
        j1.setLocalTranslation(new Vector3f(0, 0.5f, 0));
        j1.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI * 0.3f, Vector3f.UNIT_Z));
        j2.setLocalTranslation(new Vector3f(0, 0.2f, 0));
        Joint[] joints = new Joint[]{root, j1, j2};

        Armature armature = new Armature(joints);
        armature.setBindPose();

        ArmatureControl ac = new ArmatureControl(armature);
        Node node = new Node("Test Armature");
        rootNode.attachChild(node);

        node.addControl(ac);

        ArmatureDebugAppState debugAppState = new ArmatureDebugAppState();
        debugAppState.addArmature(ac, true);
        stateManager.attach(debugAppState);

        rootNode.addLight(new DirectionalLight(new Vector3f(-1f, -1f, -1f).normalizeLocal()));

        rootNode.addLight(new DirectionalLight(new Vector3f(1f, 1f, 1f).normalizeLocal(), new ColorRGBA(0.7f, 0.7f, 0.7f, 1.0f)));


        flyCam.setEnabled(false);

        ChaseCameraAppState chaseCam = new ChaseCameraAppState();
        chaseCam.setTarget(node);
        getStateManager().attach(chaseCam);
        chaseCam.setInvertHorizontalAxis(true);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setZoomSpeed(0.5f);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI);
        chaseCam.setRotationSpeed(3);
        chaseCam.setDefaultDistance(3);
        chaseCam.setMinDistance(0.01f);
        chaseCam.setZoomSpeed(0.01f);
        chaseCam.setDefaultVerticalRotation(0.3f);
    }


    private void displayNormals(Spatial s) {
        final Node debugTangents = new Node("debug tangents");
        debugTangents.setCullHint(Spatial.CullHint.Never);

        rootNode.attachChild(debugTangents);

        final Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        debugMat.getAdditionalRenderState().setLineWidth(2);

        s.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry g) {
                Mesh m = g.getMesh();
                Geometry debug = new Geometry(
                        "debug tangents geom",
                        TangentBinormalGenerator.genNormalLines(m, 0.1f)
                );
                debug.setMaterial(debugMat);
                debug.setCullHint(Spatial.CullHint.Never);
                debug.setLocalTransform(g.getWorldTransform());
                debugTangents.attachChild(debug);
            }
        });
    }

    float time = 0;

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        float rot = FastMath.sin(time);
        j1.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI * rot, Vector3f.UNIT_Z));
        j2.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI * rot, Vector3f.UNIT_Z));

    }
}
