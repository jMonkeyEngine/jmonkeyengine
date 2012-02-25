/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.model.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Torus;

public class TestExpandingTorus extends SimpleApplication {

    private float outerRadius = 1.5f;
    private float rate = 1;
    private Torus torus;
    private Geometry geom;
    
    public static void main(String[] args) {
        TestExpandingTorus app = new TestExpandingTorus();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        torus = new Torus(30, 10, .5f, 1f);
        geom = new Geometry("Torus", torus);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }
    
    @Override
    public void simpleUpdate(float tpf){
        if (outerRadius > 2.5f){
            outerRadius = 2.5f;
            rate = -rate;
        }else if (outerRadius < 1f){
            outerRadius = 1f;
            rate = -rate;
        }
        outerRadius += rate * tpf;
        torus.updateGeometry(30, 10, .5f, outerRadius);
    }
}