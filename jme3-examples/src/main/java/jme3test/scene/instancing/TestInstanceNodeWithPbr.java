package jme3test.scene.instancing;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;

public class TestInstanceNodeWithPbr extends SimpleApplication {
    // Try to test with different offset
    private static float offset = 12;

    public static void main(String[] args) {
        TestInstanceNodeWithPbr app = new TestInstanceNodeWithPbr();
        app.start();
    }

    private Geometry box;
    private PointLight pointLight;

    @Override
    public void simpleInitApp() {
        InstancedNode instancedNode = new InstancedNode("testInstancedNode");
        rootNode.attachChild(instancedNode);

        box = new Geometry("PBRLightingBox", new Box(0.5f, 0.5f, 0.5f));
        Material pbrLightingMaterial = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        pbrLightingMaterial.setBoolean("UseInstancing", true);
        pbrLightingMaterial.setColor("BaseColor", ColorRGBA.Red);
        box.setMaterial(pbrLightingMaterial);

        instancedNode.attachChild(box);
        instancedNode.instance();

        pointLight = new PointLight();
        pointLight.setColor(ColorRGBA.White);
        pointLight.setRadius(10f);
        rootNode.addLight(pointLight);

        box.setLocalTranslation(new Vector3f(offset, 0, 0));
        pointLight.setPosition(new Vector3f(offset - 3f, 0, 0));

        cam.setLocation(new Vector3f(offset - 5f, 0, 0));
        cam.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        offset += tpf;

        System.err.println(offset);
        box.setLocalTranslation(new Vector3f(offset, 0, 0));
        pointLight.setPosition(new Vector3f(offset - 3f, 0, 0));

        cam.setLocation(new Vector3f(offset - 5f, 0, 0));
        cam.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
    }
}