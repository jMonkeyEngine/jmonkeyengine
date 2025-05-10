package jme3test.scene.instancing;

import java.util.Locale;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;

/**
 * This test specifically validates the corrected PBR rendering when combined
 * with instancing, as addressed in issue #2435. 
 * 
 * It creates an InstancedNode
 * with a PBR-materialized Box to ensure the fix in PBRLighting.vert correctly
 * handles world position calculations for instanced geometry.
 */
public class TestInstanceNodeWithPbr extends SimpleApplication {

    public static void main(String[] args) {
        TestInstanceNodeWithPbr app = new TestInstanceNodeWithPbr();
        app.start();
    }

    private BitmapText bmp;
    private Geometry box;
    private float pos = -5;
    private float vel = 5;
    
    @Override
    public void simpleInitApp() {
        configureCamera();
        bmp = createLabelText(10, 20, "<placeholder>");
        
        InstancedNode instancedNode = new InstancedNode("InstancedNode");
        rootNode.attachChild(instancedNode);

        Box mesh = new Box(0.5f, 0.5f, 0.5f);
        box = new Geometry("Box", mesh);
        Material pbrMaterial = createPbrMaterial(ColorRGBA.Red);
        box.setMaterial(pbrMaterial);

        instancedNode.attachChild(box);
        instancedNode.instance();

        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
        rootNode.addLight(light);
    }

    private Material createPbrMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", color);
        mat.setFloat("Roughness", 0.8f);
        mat.setFloat("Metallic", 0.1f);
        mat.setBoolean("UseInstancing", true);
        return mat;
    }
    
    private void configureCamera() {
        flyCam.setMoveSpeed(15f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
    private BitmapText createLabelText(int x, int y, String text) {
        BitmapText bmp = new BitmapText(guiFont);
        bmp.setText(text);
        bmp.setLocalTranslation(x, settings.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
        return bmp;
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        pos += tpf * vel;
        if (pos < -10f || pos > 10f) {
            vel *= -1;
        }
        box.setLocalTranslation(pos, 0f, 0f);
        bmp.setText(String.format(Locale.ENGLISH, "BoxPosition: (%.2f, %.1f, %.1f)", pos, 0f, 0f));
    }

}
