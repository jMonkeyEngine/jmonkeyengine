package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class TestUnshadedModel extends SimpleApplication {

    public static void main(String[] args){
        TestUnshadedModel app = new TestUnshadedModel();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Sphere sphMesh = new Sphere(32, 32, 1);
        sphMesh.setTextureMode(Sphere.TextureMode.Projected);
        sphMesh.updateGeometry(32, 32, 1, false, false);
        TangentBinormalGenerator.generate(sphMesh);

        Geometry sphere = new Geometry("Rock Ball", sphMesh);
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat.setColor("Ambient", ColorRGBA.DarkGray);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        sphere.setMaterial(mat);
        rootNode.attachChild(sphere);

        PointLight pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        pl.setPosition(new Vector3f(4f, 0f, 0f));
        rootNode.addLight(pl);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White);
        rootNode.addLight(al);
    }
}
