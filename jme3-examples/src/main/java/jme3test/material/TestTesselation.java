package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;


/**
 * Created by michael on 23.02.15.
 */
public class TestTesselation extends SimpleApplication {
    private Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    DirectionalLight dl;

    public void setupSkyBox() {
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", false));
    }


    public void setupLighting() {

        dl = new DirectionalLight();
        dl.setDirection(lightDir);
        dl.setColor(new ColorRGBA(.9f, .9f, .9f, 1));
        rootNode.addLight(dl);
    }

    void setupParallax() {
        Material mat;
        mat = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall2.j3m");
        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(Texture.WrapMode.Repeat);
        mat.getTextureParam("NormalMap").getTextureValue().setWrap(Texture.WrapMode.Repeat);

        // Node floorGeom = (Node) assetManager.loadAsset("Models/WaterTest/WaterTest.mesh.xml");
        //Geometry g = ((Geometry) floorGeom.getChild(0));
        //g.getMesh().scaleTextureCoordinates(new Vector2f(10, 10));

        Node floorGeom = new Node("floorGeom");
        Quad q = new Quad(100, 100);
        q.scaleTextureCoordinates(new Vector2f(1, 1));
        Geometry g = new Geometry("geom", q);
        g.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        floorGeom.attachChild(g);


        TangentBinormalGenerator.generate(floorGeom);
        floorGeom.setLocalTranslation(-50, 22, 60);
        //floorGeom.setLocalScale(100);

        floorGeom.setMaterial(mat);
        rootNode.attachChild(floorGeom);
    }

    @Override
    public void simpleInitApp() {
        setupLighting();
        setupParallax();
        cam.setLocation(new Vector3f(-15.445636f, 30.162927f, 60.252777f));
        cam.setRotation(new Quaternion(0.05173137f, 0.92363626f, -0.13454558f, 0.35513034f));
    }

    public static void main(String[] args){
        TestTesselation app = new TestTesselation();
        app.start();
    }
}
