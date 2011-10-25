/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package jme3test.batching;


import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.NanoTimer;
import com.jme3.util.TangentBinormalGenerator;

/**
 *
 * @author Nehon
 */
public class TestBatchNode extends SimpleApplication {

    public static void main(String[] args) {

        TestBatchNode app = new TestBatchNode();
        app.start();
    }
    BatchNode batch;

    @Override
    public void simpleInitApp() {
        timer = new NanoTimer();
        batch = new BatchNode("theBatchNode");

        /**
         * A cube with a color "bleeding" through transparent texture. Uses
         * Texture from jme3-test-data library!
         */
        Box boxshape4 = new Box(Vector3f.ZERO, 1f, 1f, 1f );
        cube = new Geometry("cube1", boxshape4);
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");     
        cube.setMaterial(mat);
//        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");        
//        mat.setColor("Diffuse", ColorRGBA.Blue);
//        mat.setBoolean("UseMaterialColors", true);
        /**
         * A cube with a color "bleeding" through transparent texture. Uses
         * Texture from jme3-test-data library!
         */
        Box box = new Box(Vector3f.ZERO, 1f, 1f, 1f);
        cube2 = new Geometry("cube2", box);
        cube2.setMaterial(mat);
        
        TangentBinormalGenerator.generate(cube);
        TangentBinormalGenerator.generate(cube2);


         n = new Node("aNode");
       // n.attachChild(cube2);
        batch.attachChild(cube);
        batch.attachChild(cube2);
      //  batch.setMaterial(mat);
        batch.batch();
        rootNode.attachChild(batch);
        cube.setLocalTranslation(3, 0, 0);
        cube2.setLocalTranslation(0, 3, 0);


        dl=new DirectionalLight();
        dl.setColor(ColorRGBA.White.mult(2));
        dl.setDirection(new Vector3f(1, -1, -1));
        rootNode.addLight(dl);
        flyCam.setMoveSpeed(10);
    }
    Node n;
    Geometry cube;
    Geometry cube2;
    float time = 0;
    DirectionalLight dl;
    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        dl.setDirection(cam.getDirection());
        cube2.setLocalTranslation(FastMath.sin(-time)*3, FastMath.cos(time)*3, 0);        
        cube2.setLocalRotation(new Quaternion().fromAngleAxis(time, Vector3f.UNIT_Z));
        cube2.setLocalScale(Math.max(FastMath.sin(time),0.5f));

        batch.setLocalRotation(new Quaternion().fromAngleAxis(time, Vector3f.UNIT_Z));
        
    }
//    
}
