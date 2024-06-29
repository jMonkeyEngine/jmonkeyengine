/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.framegraph.examples;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class HelloCustomFrameGraph extends SimpleApplication {
    
    public static void main(String[] args) {
        HelloCustomFrameGraph app = new HelloCustomFrameGraph();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(700);
        settings.setHeight(700);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        
        
        Geometry box = new Geometry("box", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        box.setMaterial(mat);
        rootNode.attachChild(box);
        
        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(2, 5, 5));
        pl.setRadius(100);
        rootNode.addLight(pl);
        
        flyCam.setMoveSpeed(15);
        flyCam.setDragToRotate(true);
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
}
