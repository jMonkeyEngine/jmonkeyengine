/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.framegraph.sandbox;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.framegraph.CartoonEdgePass;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.passes.BucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.SceneEnqueuePass;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class Main extends SimpleApplication {

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
        
        FrameGraph frameGraph = new FrameGraph(assetManager);
        
        SceneEnqueuePass enqueue = frameGraph.add(new SceneEnqueuePass());
        BucketPass opaque = frameGraph.add(new BucketPass());
        BucketPass sky = frameGraph.add(new BucketPass(DepthRange.REAR));
        BucketPass transparent = frameGraph.add(new BucketPass());
        BucketPass gui = frameGraph.add(new BucketPass(DepthRange.FRONT, false));
        BucketPass translucent = frameGraph.add(new BucketPass());
        OutputPass output = frameGraph.add(new OutputPass());
        
        opaque.makeInput(enqueue, "Opaque", "Geometry");
        
        sky.makeInput(opaque, "Color", "Color");
        sky.makeInput(opaque, "Depth", "Depth");
        sky.makeInput(enqueue, "Sky", "Geometry");
        
        transparent.makeInput(sky, "Color", "Color");
        transparent.makeInput(sky, "Depth", "Depth");
        transparent.makeInput(enqueue, "Transparent", "Geometry");
        
        gui.makeInput(transparent, "Color", "Color");
        gui.makeInput(transparent, "Depth", "Depth");
        gui.makeInput(enqueue, "Gui", "Geometry");
        
        translucent.makeInput(gui, "Color", "Color");
        translucent.makeInput(gui, "Depth", "Depth");
        translucent.makeInput(enqueue, "Translucent", "Geometry");
        
        output.makeInput(translucent, "Color", "Color");
        output.makeInput(translucent, "Depth", "Depth");
        
        viewPort.setFrameGraph(frameGraph);
        
        // setup camera
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(20);
        
        // setup background
        viewPort.setBackgroundColor(ColorRGBA.White.mult(0.02f));
        
        // add a cube to the scene
        Geometry cube = new Geometry("cube", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", ColorRGBA.White);
        mat.setFloat("Metallic", 0.5f);
        cube.setMaterial(mat);
        rootNode.attachChild(cube);
        
        // add a light to the scene
        PointLight pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        pl.setPosition(new Vector3f(2, 3, 3));
        pl.setRadius(20);
        rootNode.addLight(pl);
        
        // profiler
        DetailedProfilerState profiler = new DetailedProfilerState();
        profiler.setEnabled(false);
        stateManager.attach(profiler);
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
}
