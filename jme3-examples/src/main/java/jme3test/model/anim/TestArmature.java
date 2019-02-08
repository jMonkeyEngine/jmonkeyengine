package jme3test.model.anim;

import com.jme3.anim.*;
import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.debug.custom.ArmatureDebugAppState;
import com.jme3.scene.shape.Cylinder;
import com.jme3.util.TangentBinormalGenerator;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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
        setTimer(new EraseTimer());
        renderManager.setSinglePassLightBatchSize(2);
        //cam.setFrustumPerspective(90f, (float) cam.getWidth() / cam.getHeight(), 0.01f, 10f);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        //create armature
        Joint root = new Joint("Root_Joint");
        j1 = new Joint("Joint_1");
        j2 = new Joint("Joint_2");
        Joint j3 = new Joint("Joint_3");
        root.addChild(j1);
        j1.addChild(j2);
        j2.addChild(j3);
        root.setLocalTranslation(new Vector3f(0, 0, 0.5f));
        j1.setLocalTranslation(new Vector3f(0, 0.0f, -0.5f));
        j2.setLocalTranslation(new Vector3f(0, 0.0f, -0.3f));
        j3.setLocalTranslation(new Vector3f(0, 0, -0.2f));
        Joint[] joints = new Joint[]{root, j1, j2, j3};

        final Armature armature = new Armature(joints);
        //armature.setModelTransformClass(SeparateJointModelTransform.class);
        armature.saveBindPose();

        //create animations
        AnimClip clip = new AnimClip("anim");
        float[] times = new float[]{0, 2, 4};
        Quaternion[] rotations = new Quaternion[]{
                new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X),
                new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X),
                new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X)
        };
        Vector3f[] translations = new Vector3f[]{
                new Vector3f(0, 0.2f, 0),
                new Vector3f(0, 1.0f, 0),
                new Vector3f(0, 0.2f, 0),
        };
        Vector3f[] scales = new Vector3f[]{
                new Vector3f(1, 1, 1),
                new Vector3f(1, 1, 2),
                new Vector3f(1, 1, 1),
        };
        Vector3f[] scales2 = new Vector3f[]{
                new Vector3f(1, 1, 1),
                new Vector3f(1, 1, 0.5f),
                new Vector3f(1, 1, 1),
        };

        TransformTrack track1 = new TransformTrack(j1, times, null, rotations, scales);
        TransformTrack track2 = new TransformTrack(j2, times, null, rotations, null);

        clip.setTracks(new TransformTrack[]{track1, track2});

        //create the animComposer control
        final AnimComposer composer = new AnimComposer();
        composer.addAnimClip(clip);

        //create the SkinningControl
        SkinningControl ac = new SkinningControl(armature);
        ac.setHardwareSkinningPreferred(false);
        Node node = new Node("Test Armature");

        rootNode.attachChild(node);

        //Create the mesh to deform.
        Geometry cylinder = new Geometry("cylinder", createMesh());
        Material m = new Material(assetManager, "Common/MatDefs/Misc/fakeLighting.j3md");
        m.setColor("Color", ColorRGBA.randomColor());
        cylinder.setMaterial(m);
        node.attachChild(cylinder);
        node.addControl(composer);
        node.addControl(ac);

        composer.setCurrentAction("anim");

        ArmatureDebugAppState debugAppState = new ArmatureDebugAppState();
        debugAppState.addArmatureFrom(ac);
        stateManager.attach(debugAppState);

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


        inputManager.addMapping("bind", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    composer.reset();
                    armature.applyBindPose();

                } else {
                    composer.setCurrentAction("anim");
                }
            }
        }, "bind");
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

    private Mesh createMesh() {
        Cylinder c = new Cylinder(30, 16, 0.1f, 1, true);

        ShortBuffer jointIndex = (ShortBuffer) VertexBuffer.createBuffer(VertexBuffer.Format.UnsignedShort, 4, c.getVertexCount());
        jointIndex.rewind();
        c.setMaxNumWeights(1);
        FloatBuffer jointWeight = (FloatBuffer) VertexBuffer.createBuffer(VertexBuffer.Format.Float, 4, c.getVertexCount());
        jointWeight.rewind();
        VertexBuffer vb = c.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer fvb = (FloatBuffer) vb.getData();
        fvb.rewind();
        for (int i = 0; i < c.getVertexCount(); i++) {
            fvb.get();
            fvb.get();
            float z = fvb.get();
            int index = 0;
            if (z > 0) {
                index = 0;
            } else if (z > -0.2) {
                index = 1;
            } else {
                index = 2;
            }
            jointIndex.put((short) index).put((short) 0).put((short) 0).put((short) 0);
            jointWeight.put(1f).put(0f).put(0f).put(0f);

        }
        c.setBuffer(VertexBuffer.Type.BoneIndex, 4, jointIndex);
        c.setBuffer(VertexBuffer.Type.BoneWeight, 4, jointWeight);

        c.updateCounts();
        c.updateBound();

        VertexBuffer weightsHW = new VertexBuffer(VertexBuffer.Type.HWBoneWeight);
        VertexBuffer indicesHW = new VertexBuffer(VertexBuffer.Type.HWBoneIndex);

        indicesHW.setUsage(VertexBuffer.Usage.CpuOnly);
        weightsHW.setUsage(VertexBuffer.Usage.CpuOnly);
        c.setBuffer(weightsHW);
        c.setBuffer(indicesHW);
        c.generateBindPose();

        c.prepareForAnim(false);

        return c;
    }


}
