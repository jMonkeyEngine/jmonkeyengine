/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.renderpath.sandbox;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.FrameGraphFactory;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class Main extends SimpleApplication {
    
    private final boolean pbr = true;
    
    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        //stateManager.attach(new DetailedProfilerState());
        
        FrameGraph deferred = FrameGraphFactory.deferred(assetManager, false);
        deferred.setJunctionSetting("LightPackMethod", true);
        //viewPort.setFrameGraph(deferred);
        
        // set camera move speed
        flyCam.setMoveSpeed(30);
        flyCam.setDragToRotate(true);
        viewPort.setBackgroundColor(ColorRGBA.White.mult(0.05f));
                
        // add a large floor to the scene
        RectangleMesh floorMesh = new RectangleMesh(
                new Vector3f(-100, 0, -100), new Vector3f(100, 0, -100), new Vector3f(-100, 0, 100));
        floorMesh.flip();
        Geometry floor = new Geometry("Floor", floorMesh);
        floor.setLocalTranslation(0, -5, 0);
        Material mat = createMaterial(ColorRGBA.White);
        floor.setMaterial(mat);
        rootNode.attachChild(floor);
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -1, 1));
        //rootNode.addLight(dl);
        
        // add a lot of boxes to the scene
        for (int i = 0; i < 500; i++) {
            Geometry box = new Geometry("Box", new Box(1, 1, 1));
            box.setLocalTranslation(FastMath.rand.nextFloat(-100, 100), 0, FastMath.rand.nextFloat(-100, 100));
            box.setMaterial(createMaterial(ColorRGBA.White));
            rootNode.attachChild(box);
            EnvironmentProbeControl.tagGlobal(box);
        }
        
        // add some lights to the scene
        for (int i = 0; i < 100; i++) {
            PointLight pl = new PointLight();
            pl.setPosition(new Vector3f(FastMath.rand.nextFloat(-100, 100), 5, FastMath.rand.nextFloat(-100, 100)));
            pl.setRadius(100);
            pl.setColor(ColorRGBA.randomColor());
            rootNode.addLight(pl);
        }
        
        //rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
        //rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0f)));
        
    }
    
    private Material createMaterial(ColorRGBA color) {
        if (pbr) {
            Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
            mat.setColor("BaseColor", color);
            mat.setFloat("Metallic", 1.0f);
            return mat;
        } else {
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", color);
            return mat;
        }
    }
    
}
