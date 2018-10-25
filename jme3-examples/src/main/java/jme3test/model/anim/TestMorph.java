package jme3test.model.anim;

import com.jme3.anim.MorphControl;
import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;

public class TestMorph extends SimpleApplication {

    float[] weights = new float[2];

    public static void main(String... args) {
        TestMorph app = new TestMorph();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        final Box box = new Box(1, 1, 1);
        FloatBuffer buffer = BufferUtils.createVector3Buffer(box.getVertexCount());

        float[] d = new float[box.getVertexCount() * 3];
        for (int i = 0; i < d.length; i++) {
            d[i] = 0;
        }

        d[12] = 1f;
        d[15] = 1f;
        d[18] = 1f;
        d[21] = 1f;

        buffer.put(d);
        buffer.rewind();

        MorphTarget target = new MorphTarget();
        target.setBuffer(VertexBuffer.Type.Position, buffer);
        box.addMorphTarget(target);


        buffer = BufferUtils.createVector3Buffer(box.getVertexCount());

        for (int i = 0; i < d.length; i++) {
            d[i] = 0;
        }

        d[13] = 1f;
        d[16] = 1f;
        d[19] = 1f;
        d[22] = 1f;

        buffer.put(d);
        buffer.rewind();

        final MorphTarget target2 = new MorphTarget();
        target2.setBuffer(VertexBuffer.Type.Position, buffer);
        box.addMorphTarget(target2);

        final Geometry g = new Geometry("box", box);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        g.setMaterial(m);
        m.setColor("Color", ColorRGBA.Red);
        m.setInt("NumberOfMorphTargets", 2);

        rootNode.attachChild(g);

        g.setMorphState(weights);
        g.addControl(new MorphControl());

        ChaseCameraAppState chase = new ChaseCameraAppState();
        chase.setTarget(rootNode);
        getStateManager().attach(chase);
        flyCam.setEnabled(false);

        inputManager.addMapping("morphright", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("morphleft", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("morphup", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("morphdown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("change", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                if (name.equals("morphleft")) {
                    weights[0] -= tpf;
                }
                if (name.equals("morphright")) {
                    weights[0] += tpf;
                }
                if (name.equals("morphup")) {
                    weights[1] += tpf;
                }
                if (name.equals("morphdown")) {
                    weights[1] -= tpf;
                }
                g.setMorphState(weights);

            }
        }, "morphup", "morphdown", "morphleft", "morphright");

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("change") && isPressed) {
                    box.setBuffer(VertexBuffer.Type.MorphTarget0, 3, target2.getBuffer(VertexBuffer.Type.Position));
                }
            }
        }, "change");
    }
}
