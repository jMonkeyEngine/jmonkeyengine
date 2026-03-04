package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public class TestGlobalLight extends SimpleApplication {
    public static void main(String[] args) {
        TestGlobalLight test = new TestGlobalLight();
        test.start();
    }

    private static Geometry createLitWhiteCube(AssetManager assetManager, String name) {
        Box box = new Box(1, 1, 1);
        Geometry cube = new Geometry(name, box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        cube.setMaterial(mat);
        return cube;
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        PointLight globalPointLight = new PointLight(true);
        Node lightsAttachedNode = new Node("lightsAttachedNode");

        Node lightsNotAttachedNode = new Node("lightsNotAttachedNode");

        Geometry litByAll = createLitWhiteCube(getAssetManager(), "litByAll");
        litByAll.setLocalTranslation(2, 0, -1);
        lightsAttachedNode.attachChild(litByAll);

        Geometry litOnlyByGlobal = createLitWhiteCube(getAssetManager(), "litOnlyByGlobal");
        litOnlyByGlobal.setLocalTranslation(-2, 0, -1);
        lightsNotAttachedNode.attachChild(litOnlyByGlobal);

        PointLight localPointLight = new PointLight();
        localPointLight.setColor(ColorRGBA.Red);

        globalPointLight.setColor(ColorRGBA.Green);
        
        Vector3f testOffset = new Vector3f(0, 0, 0);
        getCamera().setLocation(testOffset.add(new Vector3f(0, 0, 10)));
        getCamera().lookAt(testOffset, Vector3f.UNIT_Y);

        Node rootNode = new Node();
        rootNode.attachChild(lightsAttachedNode);
        rootNode.attachChild(lightsNotAttachedNode);
      
        Node rootRootNode = new Node();
        getRootNode().attachChild(rootRootNode);

        lightsAttachedNode.addLight(localPointLight);
        lightsAttachedNode.addLight(globalPointLight);
        
        rootRootNode.attachChild(rootNode);

        getRootNode().addLight(new AmbientLight(new ColorRGBA(0.01f, 0.01f, 0.01f, 1)));
    }
}
