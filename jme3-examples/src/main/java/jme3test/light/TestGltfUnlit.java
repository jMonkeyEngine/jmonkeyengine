package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * Tests the GLTF scene containing an unlit material. If it works, it should use the
 * Common/MatDefs/Misc/Unshaded.j3md material definition for those objects.
 *
 * @author Markil 3
 * @version 3.3.0-SNAPSHOT
 */
public class TestGltfUnlit extends SimpleApplication implements ActionListener {
    public static void main(String[] args) {
        TestGltfUnlit testUnlit = new TestGltfUnlit();
        testUnlit.start();
    }

    @Override
    public void simpleInitApp() {

        ColorRGBA skyColor = new ColorRGBA(0.5f, 0.6f, 0.7f, 0.0f);

        flyCam.setMoveSpeed(20);
        viewPort.setBackgroundColor(skyColor.mult(0.9f));

        this.cam.setLocation(new Vector3f(0, 10, 20));
        this.rootNode.attachChild(this.getAssetManager().loadModel("jme3test/scenes/unlit.gltf"));
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
    }
}